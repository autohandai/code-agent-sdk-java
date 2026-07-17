package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Typed contracts for Autohand's persisted, replayable autoresearch ledger.
 *
 * <p>The nested records mirror the CLI JSON-RPC payloads while keeping the
 * related public API discoverable under one namespace.</p>
 */
public final class Autoresearch {
    private Autoresearch() {
    }

    public enum OptimizationDirection {
        LOWER("lower"), HIGHER("higher");

        private final String value;

        OptimizationDirection(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum ConstraintOperator {
        LESS_THAN("<"), LESS_THAN_OR_EQUAL("<="), GREATER_THAN(">"), GREATER_THAN_OR_EQUAL(">=");

        private final String value;

        ConstraintOperator(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum EvaluatorMode {
        ORIGINAL("original"), CURRENT("current");

        private final String value;

        EvaluatorMode(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum MaterializationState {
        BASELINE("baseline"), COMMITTED("committed"), RETAINED("retained"), REVERTED("reverted"), NONE("none");

        private final String value;

        MaterializationState(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public record SubagentOptions(Boolean ideaGeneration, Boolean measurementAnalysis, Boolean finalization) {
    }

    public record SecondaryObjective(String name, String unit, OptimizationDirection direction) {
        public SecondaryObjective {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(unit, "unit");
            Objects.requireNonNull(direction, "direction");
        }
    }

    public record Constraint(String metricName, ConstraintOperator operator, double threshold) {
        public Constraint {
            Objects.requireNonNull(metricName, "metricName");
            Objects.requireNonNull(operator, "operator");
        }
    }

    public record SamplingOptions(Integer minSamples, Integer maxSamples, Double confidenceThreshold) {
    }

    public record RetentionOptions(Long maxArtifactBytes, Integer maxArtifactAgeDays) {
    }

    public record StartParams(
            String objective,
            Integer maxIterations,
            Long timeoutMs,
            String metricName,
            String metricUnit,
            OptimizationDirection direction,
            String measureCommand,
            String measureScript,
            String checksCommand,
            String checksScript,
            List<String> filesInScope,
            SubagentOptions subagents,
            List<SecondaryObjective> secondaryObjectives,
            List<Constraint> constraints,
            SamplingOptions sampling,
            RetentionOptions retention,
            List<String> environmentAllowlist
    ) {
        public StartParams {
            if (objective == null || objective.isBlank()) {
                throw new IllegalArgumentException("Autoresearch objective cannot be blank.");
            }
            filesInScope = copy(filesInScope);
            secondaryObjectives = copy(secondaryObjectives);
            constraints = copy(constraints);
            environmentAllowlist = copy(environmentAllowlist);
        }

        public static Builder builder(String objective) {
            return new Builder(objective);
        }

        public static final class Builder {
            private final String objective;
            private Integer maxIterations;
            private Long timeoutMs;
            private String metricName;
            private String metricUnit;
            private OptimizationDirection direction;
            private String measureCommand;
            private String measureScript;
            private String checksCommand;
            private String checksScript;
            private List<String> filesInScope = List.of();
            private SubagentOptions subagents;
            private List<SecondaryObjective> secondaryObjectives = List.of();
            private List<Constraint> constraints = List.of();
            private SamplingOptions sampling;
            private RetentionOptions retention;
            private List<String> environmentAllowlist = List.of();

            private Builder(String objective) {
                this.objective = objective;
            }

            public Builder maxIterations(Integer value) { maxIterations = value; return this; }
            public Builder timeoutMs(Long value) { timeoutMs = value; return this; }
            public Builder metricName(String value) { metricName = value; return this; }
            public Builder metricUnit(String value) { metricUnit = value; return this; }
            public Builder direction(OptimizationDirection value) { direction = value; return this; }
            public Builder measureCommand(String value) { measureCommand = value; return this; }
            public Builder measureScript(String value) { measureScript = value; return this; }
            public Builder checksCommand(String value) { checksCommand = value; return this; }
            public Builder checksScript(String value) { checksScript = value; return this; }
            public Builder filesInScope(List<String> value) { filesInScope = copy(value); return this; }
            public Builder subagents(SubagentOptions value) { subagents = value; return this; }
            public Builder secondaryObjectives(List<SecondaryObjective> value) { secondaryObjectives = copy(value); return this; }
            public Builder constraints(List<Constraint> value) { constraints = copy(value); return this; }
            public Builder sampling(SamplingOptions value) { sampling = value; return this; }
            public Builder retention(RetentionOptions value) { retention = value; return this; }
            public Builder environmentAllowlist(List<String> value) { environmentAllowlist = copy(value); return this; }

            public StartParams build() {
                return new StartParams(objective, maxIterations, timeoutMs, metricName, metricUnit, direction,
                        measureCommand, measureScript, checksCommand, checksScript, filesInScope, subagents,
                        secondaryObjectives, constraints, sampling, retention, environmentAllowlist);
            }
        }
    }

    public record MetricAggregate(double median, double mad, int sampleCount) {
    }

    public record EvaluationSample(
            int sequence,
            Map<String, Double> metrics,
            String outputObject,
            long durationMs,
            String timestamp
    ) {
    }

    public record Checks(boolean passed, String outputObject) {
    }

    public record Execution(String outcome, String error, String outputObject) {
    }

    public record EvaluationRecord(
            int schemaVersion,
            String type,
            String id,
            String attemptId,
            String timestamp,
            Map<String, JsonNode> context,
            EvaluatorMode evaluatorMode,
            List<EvaluationSample> samples,
            Map<String, MetricAggregate> aggregates,
            Checks checks,
            Execution execution,
            List<String> driftWarnings
    ) {
    }

    public record ConstraintResult(
            String metricName,
            ConstraintOperator operator,
            double threshold,
            double conservativeValue,
            boolean passed,
            boolean conclusive
    ) {
    }

    public record DecisionRecord(
            int schemaVersion,
            String type,
            String id,
            String attemptId,
            String timestamp,
            Map<String, JsonNode> context,
            String policyVersion,
            String evaluationId,
            String source,
            List<ConstraintResult> constraintResults,
            double primaryImprovement,
            double confidence,
            String outcome,
            boolean materialized,
            String explanation
    ) {
    }

    public record HistoryAttempt(
            String attemptId,
            String description,
            String timestamp,
            boolean legacy,
            boolean replayable,
            boolean pinned,
            EvaluationRecord latestEvaluation,
            DecisionRecord latestDecision,
            MaterializationState materialization
    ) {
    }

    public record State(boolean active, String goal, int iteration, int maxIterations) {
    }

    public record StartResult(
            boolean success,
            String message,
            String instruction,
            Boolean active,
            State state,
            String statusText,
            Integer runsLogged,
            List<HistoryAttempt> attempts,
            List<String> paretoAttemptIds,
            String error
    ) {
    }

    public record StatusResult(
            boolean success,
            boolean active,
            State state,
            String statusText,
            int runsLogged,
            List<HistoryAttempt> attempts,
            List<String> paretoAttemptIds,
            String error
    ) {
    }

    public record StopResult(
            boolean success,
            String message,
            Boolean active,
            State state,
            String statusText,
            Integer runsLogged,
            List<HistoryAttempt> attempts,
            List<String> paretoAttemptIds,
            String error
    ) {
    }

    public record HistoryResult(boolean success, List<HistoryAttempt> attempts, String error) {
    }

    public record ReplayParams(String attemptId, EvaluatorMode evaluator) {
        public ReplayParams {
            if (attemptId == null || attemptId.isBlank()) {
                throw new IllegalArgumentException("Autoresearch replay attemptId cannot be blank.");
            }
        }
    }

    public record ReplayResult(
            boolean success,
            String attemptId,
            EvaluatorMode evaluatorMode,
            Map<String, Double> metrics,
            List<EvaluationSample> samples,
            DecisionRecord decision,
            List<String> driftWarnings,
            String error
    ) {
    }

    public record RescoreParams(String attemptId, Boolean all) {
        public RescoreParams {
            boolean oneAttempt = attemptId != null && !attemptId.isBlank();
            if (oneAttempt == Boolean.TRUE.equals(all)) {
                throw new IllegalArgumentException("Specify either attemptId or all=true for autoresearch rescore.");
            }
        }

        public static RescoreParams attempt(String attemptId) {
            return new RescoreParams(attemptId, null);
        }

        public static RescoreParams allAttempts() {
            return new RescoreParams(null, true);
        }
    }

    public record RescoreResult(boolean success, List<DecisionRecord> decisions, String error) {
    }

    public record CompareParams(String leftAttemptId, String rightAttemptId) {
        public CompareParams {
            Objects.requireNonNull(leftAttemptId, "leftAttemptId");
            Objects.requireNonNull(rightAttemptId, "rightAttemptId");
        }
    }

    public record ComparisonSide(
            String attemptId,
            List<EvaluationSample> samples,
            Map<String, MetricAggregate> aggregates,
            Checks checks,
            Execution execution,
            DecisionRecord decision
    ) {
    }

    public record Comparison(ComparisonSide left, ComparisonSide right) {
    }

    public record CompareResult(boolean success, Comparison comparison, String error) {
    }

    public record ParetoResult(boolean success, List<String> attemptIds, String error) {
    }

    public record PinParams(String attemptId, boolean pinned) {
        public PinParams {
            Objects.requireNonNull(attemptId, "attemptId");
        }
    }

    public record PinResult(boolean success, String attemptId, boolean pinned, String error) {
    }

    public record PruneParams(Boolean dryRun, Boolean yes) {
        public static PruneParams preview() {
            return new PruneParams(true, null);
        }

        public static PruneParams apply() {
            return new PruneParams(false, true);
        }
    }

    public record PruneCandidate(
            String attemptId,
            List<String> objects,
            long bytes,
            @JsonProperty("protected") boolean protectedArtifact,
            String reason
    ) {
    }

    public record PruneResult(
            boolean success,
            boolean applied,
            List<PruneCandidate> candidates,
            long bytesFreed,
            long remainingBytes,
            String error
    ) {
    }

    private static <T> List<T> copy(List<T> values) {
        return values == null ? List.of() : List.copyOf(new ArrayList<>(values));
    }
}
