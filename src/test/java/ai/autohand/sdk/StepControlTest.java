package ai.autohand.sdk;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.PromptParams;
import ai.autohand.sdk.types.SDKConfig;
import ai.autohand.sdk.types.StopConditions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StepControlTest {
    @TempDir Path directory;

    @Test
    void plainPromptWaitsForCompletedTurn() throws Exception {
        try (var sdk = new AutohandSDK(config(Map.of()))) {
            sdk.start();
            sdk.prompt(new PromptParams("Read the marker"));
            assertTrue(Files.exists(directory.resolve("completed")), "An acknowledgement is not completion");
        }
    }

    @Test
    void stopsAfterPersistedStepsAndResumesTheSameAgent() throws Exception {
        try (var agent = Agent.create(config(Map.of()))) {
            var result = agent.send(new PromptParams("Read evidence").withStopWhen(
                    StopConditions.isStepCount(2), StopConditions.hasToolCall("never"), context -> {
                        var step = context.steps().getLast();
                        assertEquals("evidence-" + step.stepNumber(), step.toolResults().getFirst().output());
                        assertThrows(UnsupportedOperationException.class, () -> context.steps().clear());
                        return CompletableFuture.completedFuture(false);
                    })).waitForResult();
            assertEquals("stopped", result.status());
            assertEquals(2, result.steps().size());
            assertEquals("read_file", result.steps().getFirst().toolCalls().getFirst().tool());
            var next = agent.run("Continue");
            assertEquals("completed", next.status());
            assertEquals("continued", next.text());
            assertTrue(next.steps().isEmpty());
            assertTrue(Files.readString(directory.resolve("requests")).contains("\"stopWhen\":{\"mode\":\"host\"}"));
        }
    }

    @Test
    void hasToolCallStopsAtTheMatchingStep() throws Exception {
        try (var agent = Agent.create(config(Map.of()))) {
            var result = agent.send(new PromptParams("Read evidence")
                    .withStopWhen(StopConditions.hasToolCall("read_file"))).waitForResult();
            assertEquals("stopped", result.status());
            assertEquals(1, result.steps().size());
        }
    }

    @Test
    void abortsAnUnresolvedPredicateWithoutAStaleDecision() throws Exception {
        var entered = new CountDownLatch(1);
        var decision = new CompletableFuture<Boolean>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
             var agent = Agent.create(config(Map.of()))) {
            var run = agent.send(new PromptParams("Read evidence").withStopWhen(context -> {
                entered.countDown();
                return decision;
            }));
            var result = executor.submit(run::waitForResult);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            run.abort();
            assertEquals("aborted", result.get(3, TimeUnit.SECONDS).status());
            assertEquals("continued", agent.run("Continue").text());
            decision.complete(true);
            assertFalse(Files.readString(directory.resolve("requests")).contains("autohand.stepDecision"));
        }
    }

    @Test
    void cancellingAQueuedRunDoesNotAbortAnotherRun() throws Exception {
        var entered = new CountDownLatch(1);
        var decision = new CompletableFuture<Boolean>();
        var queuedThread = new AtomicReference<Thread>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor();
             var agent = Agent.create(config(Map.of()))) {
            var first = agent.send(new PromptParams("Read evidence").withStopWhen(context -> {
                entered.countDown();
                return decision;
            }));
            var firstResult = executor.submit(first::waitForResult);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var queued = agent.send("Queued prompt");
            var queuedResult = executor.submit(() -> {
                queuedThread.set(Thread.currentThread());
                return queued.waitForResult();
            });
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while ((queuedThread.get() == null || queuedThread.get().getState() != Thread.State.WAITING)
                    && System.nanoTime() < deadline) Thread.sleep(5);
            assertNotNull(queuedThread.get());
            assertEquals(Thread.State.WAITING, queuedThread.get().getState());
            try {
                queued.abort();
                assertFalse(Files.readString(directory.resolve("requests")).contains("autohand.abort"));
            } finally {
                decision.complete(true);
            }
            assertEquals("stopped", firstResult.get(3, TimeUnit.SECONDS).status());
            assertEquals("aborted", queuedResult.get(3, TimeUnit.SECONDS).status());
            assertEquals(1, Files.readAllLines(directory.resolve("requests")).stream()
                    .filter(line -> line.contains("autohand.prompt")).count());
        }
    }

    @Test
    void propagatesPredicateFailureAfterStoppingAndDoesNotWaitForOtherPredicates() throws Exception {
        var failure = new IllegalStateException("predicate failed");
        try (var agent = Agent.create(config(Map.of()))) {
            var run = agent.send(new PromptParams("Read evidence").withStopWhen(
                    context -> new CompletableFuture<>(),
                    context -> CompletableFuture.failedFuture(failure)));
            assertSame(failure, assertThrows(IllegalStateException.class, run::waitForResult));
            assertTrue(Files.readString(directory.resolve("requests")).contains("\"stop\":true"));
            assertEquals("continued", agent.run("Continue").text());
        }
    }

    @Test
    void rejectsInvalidDecisionsAndRecoversTheNextTurn() throws Exception {
        for (String response : List.of("{}", "{\"success\":false}", "{\"success\":\"true\"}", "rpc-error")) {
            try (var agent = Agent.create(config(Map.of("DECISION_RESULT", response)))) {
                var run = agent.send(new PromptParams("Read evidence").withStopWhen(StopConditions.isStepCount(1)));
                var error = assertThrows(RuntimeException.class, run::waitForResult);
                assertTrue(error.getMessage().contains("stepDecision"), error.toString());
                assertEquals("continued", agent.run("Continue").text());
                assertTrue(Files.readString(directory.resolve("requests")).contains("autohand.abort"));
            }
        }
    }

    @Test
    void malformedStepsFailWithoutLeakingTheTurn() throws Exception {
        for (String step : List.of("{}", "{\"stepId\":\"one\",\"timestamp\":\"now\",\"step\":{\"stepNumber\":0,\"toolCalls\":[],\"toolResults\":[]}}",
                "{\"stepId\":\"one\",\"timestamp\":\"now\",\"step\":{\"stepNumber\":1,\"toolCalls\":[{\"tool\":\"read_file\",\"args\":[]}],\"toolResults\":[]}}")) {
            try (var agent = Agent.create(config(Map.of("STEP_RESULT", step)))) {
                var run = agent.send(new PromptParams("Read evidence").withStopWhen(StopConditions.isStepCount(1)));
                var error = assertThrows(RuntimeException.class, run::waitForResult);
                assertTrue(error.getMessage().contains("stepEnd"), error.toString());
                assertEquals("continued", agent.run("Continue").text());
            }
        }
    }

    @Test
    void readerExitInterruptsAnUnresolvedPredicate() throws Exception {
        try (var agent = Agent.create(config(Map.of("EXIT_AT_STEP", "1")))) {
            var run = agent.send(new PromptParams("Read evidence").withStopWhen(context -> new CompletableFuture<>()));
            assertThrows(RuntimeException.class, run::waitForResult);
        }
    }

    @Test
    void callbackFailureAbortsAndLeavesTheAgentUsable() throws Exception {
        try (var agent = Agent.create(config(Map.of()))) {
            var failure = new IllegalArgumentException("callback failed");
            var run = agent.send(new PromptParams("Read evidence").withStopWhen(StopConditions.isStepCount(1)));
            assertSame(failure, assertThrows(IllegalArgumentException.class, () -> run.stream(event -> { throw failure; })));
            assertEquals("continued", agent.run("Continue").text());
        }
    }

    @Test
    void validatesBuiltInConditionsBeforeStarting() {
        assertThrows(IllegalArgumentException.class, () -> StopConditions.isStepCount(0));
        assertThrows(IllegalArgumentException.class, () -> StopConditions.hasToolCall(" "));
        assertThrows(NullPointerException.class, () -> new PromptParams("Read").withStopWhen((ai.autohand.sdk.types.StopCondition) null));
    }

    private SDKConfig config(Map<String, String> environment) throws Exception {
        Path executable = directory.resolve("step-cli.cjs");
        Files.writeString(executable, """
                #!/usr/bin/env node
                const fs = require('node:fs');
                const lines = require('node:readline').createInterface({ input: process.stdin });
                const write = object => process.stdout.write(JSON.stringify(object) + '\\n');
                const reply = (id, result = { success: true }) => write({ jsonrpc: '2.0', id, result });
                const event = (method, params) => write({ jsonrpc: '2.0', method, params });
                let prompt = 0, step = 0;
                const end = status => event('autohand.turnEnd', { turnId: 'turn-' + prompt, reason: status === 'stopped' ? 'stop_condition' : status, timestamp: 'now' });
                const nextStep = () => {
                  ++step;
                  event('autohand.stepEnd', process.env.STEP_RESULT ? JSON.parse(process.env.STEP_RESULT) : {
                    stepId: 'step-' + step, timestamp: 'now', step: {
                      stepNumber: step, thought: 'Read evidence',
                      toolCalls: [{ id: 'read-' + step, tool: 'read_file', args: { path: 'evidence.txt' } }],
                      toolResults: [{ tool: 'read_file', success: true, output: 'evidence-' + step }]
                    }
                  });
                  if (process.env.EXIT_AT_STEP) setTimeout(() => process.exit(1), 30);
                };
                lines.on('line', line => {
                  fs.appendFileSync('requests', line + '\\n');
                  const { id, method, params } = JSON.parse(line);
                  if (method === 'autohand.prompt') {
                    ++prompt;
                    reply(id);
                    if (prompt === 1 && params.stopWhen) {
                      if (JSON.stringify(params.stopWhen) !== '{"mode":"host"}') process.exit(2);
                      nextStep();
                    } else setTimeout(() => {
                      fs.writeFileSync('completed', 'yes');
                      event('autohand.messageUpdate', { messageId: 'answer', delta: 'continued', timestamp: 'now' });
                      end('completed');
                    }, 80);
                  } else if (method === 'autohand.stepDecision') {
                    if (process.env.DECISION_RESULT === 'rpc-error') {
                      write({ jsonrpc: '2.0', id, error: { code: -32602, message: 'stepDecision rejected' } });
                    } else if (process.env.DECISION_RESULT) reply(id, JSON.parse(process.env.DECISION_RESULT));
                    else { reply(id); params.stop ? end('stopped') : nextStep(); }
                  } else if (method === 'autohand.abort') { reply(id); end('aborted'); }
                  else reply(id);
                });
                """);
        assertTrue(executable.toFile().setExecutable(true));
        Files.deleteIfExists(directory.resolve("requests"));
        return SDKConfig.builder().cwd(directory.toString()).cliPath(executable.toString())
                .timeoutMs(3_000).environment(environment).build();
    }
}
