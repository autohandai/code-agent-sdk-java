package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.AgentStep;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.PromptParams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Run {
    private final String id = "run-" + UUID.randomUUID();
    private final AutohandSDK sdk;
    private final PromptParams prompt;
    private final List<Event> events = new ArrayList<>();
    private final List<AgentStep> steps = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();
    private State state = State.NEW;
    private RuntimeException failure;
    private String status = "completed";
    private final AtomicBoolean cancellation = new AtomicBoolean();

    private enum State {
        NEW,
        RUNNING,
        COMPLETED,
        FAILED
    }

    Run(AutohandSDK sdk, String prompt) {
        this(sdk, new PromptParams(prompt));
    }

    Run(AutohandSDK sdk, PromptParams prompt) {
        this.sdk = sdk;
        this.prompt = prompt;
    }

    public void stream(Consumer<Event> onEvent) {
        synchronized (this) {
            if (state == State.COMPLETED) {
                List.copyOf(events).forEach(onEvent);
                return;
            }
            if (state == State.FAILED) {
                throw failure;
            }
            if (state == State.RUNNING) {
                throw new IllegalStateException("This run is already streaming.");
            }
            state = State.RUNNING;
        }

        try {
            sdk.streamPrompt(prompt, event -> {
                record(event);
                onEvent.accept(event);
            }, cancellation);
            synchronized (this) {
                state = State.COMPLETED;
                notifyAll();
            }
        } catch (RuntimeException exception) {
            synchronized (this) {
                failure = exception;
                state = State.FAILED;
                notifyAll();
            }
            throw exception;
        }
    }

    public RunResult waitForResult() {
        boolean shouldStart;
        synchronized (this) {
            shouldStart = state == State.NEW;
        }
        if (shouldStart) {
            stream(event -> {
            });
        }
        synchronized (this) {
            while (state == State.RUNNING) {
                try {
                    wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AutohandException("Interrupted while waiting for the run to complete.", exception);
                }
            }
            if (state == State.FAILED) {
                throw failure;
            }
            return new RunResult(id, status, text.toString(), events, steps);
        }
    }

    public <T> T json(Class<T> type) throws StructuredOutputError {
        RunResult result = waitForResult();
        if (type == String.class) {
            return type.cast(result.text());
        }
        return JsonParser.parseJsonText(result.text(), type);
    }

    String prompt() {
        return prompt.message();
    }

    /** Abort this active run, or cancel it before it starts. */
    public synchronized void abort() {
        if (state == State.NEW) {
            cancellation.set(true);
            status = "aborted";
            state = State.COMPLETED;
            notifyAll();
        } else if (state == State.RUNNING) {
            status = "aborted";
            sdk.abort(cancellation);
        }
    }

    private synchronized void record(Event event) {
        events.add(event);
        if (event instanceof Events.StepEndEvent step) steps.add(step.step());
        if (event instanceof Events.TurnEndEvent end) status = end.status();
        if (event instanceof Events.AgentEndEvent end) status = end.reason();
        if (event instanceof Events.MessageUpdateEvent update && update.delta() != null) {
            text.append(update.delta());
        } else if (event instanceof Events.MessageEndEvent end && end.content() != null && text.isEmpty()) {
            text.append(end.content());
        }
    }
}
