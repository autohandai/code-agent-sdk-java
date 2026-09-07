package ai.autohand.sdk.rpc;

import ai.autohand.sdk.sdk.RequestTimeoutException;
import ai.autohand.sdk.sdk.TransportException;
import ai.autohand.sdk.types.AgentStep;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.StopCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Owns one prompt's callbacks, asynchronous decisions, and terminal boundary. */
final class PromptTurn {
    final AtomicBoolean cancellation;
    final CompletableFuture<Void> failure = new CompletableFuture<>();
    final CompletableFuture<Void> terminal = new CompletableFuture<>();
    private final RPCClient client;
    private final Consumer<Event> consumer;
    private final List<StopCondition> conditions;
    private final List<AgentStep> steps = new ArrayList<>();
    private CompletableFuture<Void> callbacks = CompletableFuture.completedFuture(null);
    private CompletableFuture<Void> decision = CompletableFuture.completedFuture(null);
    private volatile RuntimeException conditionFailure;
    private boolean active = true;

    PromptTurn(RPCClient client, Consumer<Event> consumer, List<StopCondition> conditions, AtomicBoolean cancellation) {
        this.client = client;
        this.consumer = consumer;
        this.conditions = conditions;
        this.cancellation = cancellation;
    }

    synchronized boolean active() { return active && !cancellation.get(); }

    synchronized void cancelDecisions() { active = false; }

    synchronized void dispatch(Event event) {
        boolean ended = event instanceof Events.TurnEndEvent || event instanceof Events.AgentEndEvent;
        if (ended) active = false;
        callbacks = callbacks.thenRunAsync(() -> {
            if (event instanceof Events.UnknownEvent unknown && "autohand.stepEnd".equals(unknown.method())) {
                throw new TransportException("Malformed autohand.stepEnd notification");
            }
            consumer.accept(event);
            if (event instanceof Events.StepEndEvent step) {
                steps.add(step.step());
                evaluate(step.stepId(), new StopCondition.Context(steps));
            }
        }, RPCClient.EVENT_EXECUTOR);
        callbacks.whenComplete((unused, error) -> {
            if (error != null) fail(error);
            if (ended) terminal.complete(null);
        });
    }

    void fail(Throwable error) { failure.completeExceptionally(unwrap(error)); }

    void await(long timeoutMs) {
        awaitFuture(CompletableFuture.anyOf(terminal, failure), timeoutMs);
        CompletableFuture<Void> pending;
        synchronized (this) { pending = decision; }
        awaitFuture(pending, timeoutMs);
        synchronized (this) { pending = callbacks; }
        awaitFuture(pending, timeoutMs);
        if (conditionFailure != null) throw conditionFailure;
    }

    private void evaluate(String stepId, StopCondition.Context context) {
        if (!active()) return;
        var result = new CompletableFuture<Boolean>();
        var remaining = new AtomicInteger(conditions.size());
        var matched = new AtomicBoolean();
        if (conditions.isEmpty()) result.complete(false);
        for (StopCondition condition : conditions) {
            CompletableFuture.supplyAsync(() -> condition.shouldStop(context), RPCClient.EVENT_EXECUTOR)
                    .thenCompose(stage -> stage).whenComplete((stop, error) -> {
                        if (error != null) result.completeExceptionally(error);
                        else if (stop == null) result.completeExceptionally(
                                new IllegalArgumentException("Stop condition returned null"));
                        else if (stop) matched.set(true);
                        if (remaining.decrementAndGet() == 0) result.complete(matched.get());
                    });
        }
        result.whenComplete((stop, error) -> {
            synchronized (this) {
                if (!active()) return;
                if (error != null) conditionFailure = unwrap(error);
                decision = CompletableFuture.runAsync(() -> client.decideStep(this, stepId,
                        error != null || Boolean.TRUE.equals(stop)), RPCClient.EVENT_EXECUTOR);
                decision.whenComplete((unused, decisionError) -> {
                    if (decisionError != null) fail(decisionError);
                });
            }
        });
    }

    static void awaitFuture(CompletableFuture<?> future, long timeoutMs) {
        try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            throw new RequestTimeoutException("prompt completion", timeoutMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TransportException("Interrupted while waiting for prompt completion", exception);
        } catch (ExecutionException exception) {
            throw unwrap(exception);
        }
    }

    private static RuntimeException unwrap(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause() != null) {
            error = error.getCause();
        }
        return error instanceof RuntimeException runtime ? runtime : new TransportException("Prompt callback failed", error);
    }
}
