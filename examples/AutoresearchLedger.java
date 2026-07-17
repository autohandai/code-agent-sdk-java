import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.types.Autoresearch;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.PermissionMode;

import java.util.List;

/** Runs and inspects a persisted autoresearch experiment ledger. */
public final class AutoresearchLedger {
    private AutoresearchLedger() {
    }

    public static void main(String[] args) throws Exception {
        String target = System.getenv().getOrDefault("AUTOHAND_TARGET_REPO", ".");
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        try (Agent agent = Agent.create(AgentOptions.builder()
                .cwd(target)
                .cliPath(cliPath)
                .instructions("Keep candidates scoped, commit accepted candidates, and never push.")
                .permissionMode(PermissionMode.UNRESTRICTED)
                .build())) {
            if (!agent.supportsCommand("/autoresearch")) {
                throw new IllegalStateException("The connected CLI does not support /autoresearch.");
            }

            Autoresearch.StartParams params = Autoresearch.StartParams.builder(
                            "Reduce test runtime without regressing package validation")
                    .metricName("test_ms")
                    .metricUnit("ms")
                    .direction(Autoresearch.OptimizationDirection.LOWER)
                    .measureScript("mvn test\nprintf 'METRIC test_ms=1\\n'")
                    .checksCommand("mvn package")
                    .maxIterations(3)
                    .filesInScope(List.of("src", "pom.xml"))
                    .sampling(new Autoresearch.SamplingOptions(3, 9, 2.0))
                    .build();

            boolean started = false;
            try {
                Autoresearch.StartResult start = agent.startAutoresearch(params);
                requireSuccess("start", start.success(), start.error());
                started = true;
                if (start.instruction() == null || start.instruction().isBlank()) {
                    throw new IllegalStateException("Autoresearch start returned no loop instruction.");
                }

                var run = agent.send(start.instruction());
                run.stream(event -> {
                    if (event instanceof Events.AutoresearchLifecycleEvent lifecycle) {
                        System.out.println("[autoresearch:" + lifecycle.phase() + "] " + lifecycle.statusText());
                    } else if (event instanceof Events.AutoresearchOperationEvent operation) {
                        System.out.println("[ledger:" + operation.phase() + "] " + operation.operation());
                    } else if (event instanceof Events.MessageUpdateEvent message) {
                        System.out.print(message.delta());
                    }
                });
                run.waitForResult();

                Autoresearch.HistoryResult history = agent.getAutoresearchHistory();
                requireSuccess("history", history.success(), history.error());
                for (Autoresearch.HistoryAttempt attempt : history.attempts()) {
                    System.out.printf("%s replayable=%s materialization=%s pinned=%s%n",
                            attempt.attemptId(), attempt.replayable(), attempt.materialization(), attempt.pinned());
                }

                Autoresearch.ParetoResult pareto = agent.getAutoresearchPareto();
                requireSuccess("pareto", pareto.success(), pareto.error());
                System.out.println("Pareto attempts: " + pareto.attemptIds());

                Autoresearch.PruneResult preview = agent.pruneAutoresearch(Autoresearch.PruneParams.preview());
                requireSuccess("prune preview", preview.success(), preview.error());
                System.out.println("Prune preview candidates: " + preview.candidates().size());
            } finally {
                if (started) {
                    Autoresearch.StopResult stop = agent.stopAutoresearch();
                    if (!stop.success()) {
                        System.err.println("Unable to pause autoresearch: " + stop.error());
                    }
                }
            }
        }
    }

    private static void requireSuccess(String operation, boolean success, String error) {
        if (!success) {
            throw new IllegalStateException(operation + " failed: " + (error == null ? "unknown error" : error));
        }
    }
}
