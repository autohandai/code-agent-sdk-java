package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Map;

/** Typed contracts for persistent Autohand goals. */
public final class Goals {
    private Goals() {
    }

    public enum Status {
        ACTIVE("active"),
        PAUSED("paused"),
        BUDGET_LIMITED("budgetLimited"),
        COMPLETE("complete");

        private final String cliValue;

        Status(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }

        @JsonCreator
        public static Status fromCliValue(String value) {
            for (Status status : values()) {
                if (status.cliValue.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown goal status: " + value);
        }
    }

    public enum UpdateAction {
        UNCHANGED,
        SET,
        CLEAR
    }

    /** A nullable update that distinguishes omission from explicitly clearing a field. */
    public record NullableUpdate<T>(UpdateAction action, T value) {
        public NullableUpdate {
            if (action == null) {
                throw new IllegalArgumentException("action is required");
            }
            if (action == UpdateAction.SET && value == null) {
                throw new IllegalArgumentException("SET requires a value");
            }
        }

        public static <T> NullableUpdate<T> unchanged() {
            return new NullableUpdate<>(UpdateAction.UNCHANGED, null);
        }

        public static <T> NullableUpdate<T> set(T value) {
            return new NullableUpdate<>(UpdateAction.SET, value);
        }

        public static <T> NullableUpdate<T> clear() {
            return new NullableUpdate<>(UpdateAction.CLEAR, null);
        }
    }

    public record Budget(
            Long tokenBudget,
            Long timeBudgetSeconds,
            Long minTokensBeforeWrapUp,
            Long minTimeSecondsBeforeWrapUp
    ) {
        public static Budget none() {
            return new Budget(null, null, null, null);
        }
    }

    public record CreateParams(String objective, Budget budget) {
        public CreateParams(String objective) {
            this(objective, Budget.none());
        }
    }

    public record UpdateParams(
            String objective,
            Status status,
            NullableUpdate<Long> tokenBudget,
            NullableUpdate<Long> timeBudgetSeconds,
            NullableUpdate<Long> minTokensBeforeWrapUp,
            NullableUpdate<Long> minTimeSecondsBeforeWrapUp
    ) {
        public UpdateParams {
            tokenBudget = defaultUpdate(tokenBudget);
            timeBudgetSeconds = defaultUpdate(timeBudgetSeconds);
            minTokensBeforeWrapUp = defaultUpdate(minTokensBeforeWrapUp);
            minTimeSecondsBeforeWrapUp = defaultUpdate(minTimeSecondsBeforeWrapUp);
        }

        public static UpdateParams status(Status status) {
            return new UpdateParams(null, status, null, null, null, null);
        }

        private static <T> NullableUpdate<T> defaultUpdate(NullableUpdate<T> update) {
            return update == null ? NullableUpdate.unchanged() : update;
        }
    }

    public record GoalState(
            String goalId,
            String objective,
            Status status,
            Long tokenBudget,
            Long timeBudgetSeconds,
            Long minTokensBeforeWrapUp,
            Long minTimeSecondsBeforeWrapUp,
            long tokensUsed,
            long timeUsedSeconds,
            long createdAt,
            long updatedAt
    ) {
    }

    public record QueuedGoal(
            String queueId,
            String objective,
            Long tokenBudget,
            Long timeBudgetSeconds,
            Long minTokensBeforeWrapUp,
            Long minTimeSecondsBeforeWrapUp,
            String source,
            String template,
            Map<String, String> templateFlags,
            String templateArgs,
            long createdAt
    ) {
    }

    public record CompletedGoal(
            String goalId,
            String objective,
            Status status,
            long tokensUsed,
            long timeUsedSeconds,
            long createdAt,
            long completedAt
    ) {
    }

    public record Snapshot(
            int version,
            GoalState goal,
            List<QueuedGoal> queue,
            List<CompletedGoal> completed,
            long updatedAt
    ) {
    }

    public record SnapshotResult(boolean enabled, Snapshot snapshot, String message) {
        public static SnapshotResult enabled(Snapshot snapshot) {
            return new SnapshotResult(true, snapshot, null);
        }

        public static SnapshotResult disabled(String message) {
            return new SnapshotResult(false, null, message);
        }
    }

    public record Telemetry(Long timeRemainingSeconds, Long tokensRemaining, Boolean completionFloorMet) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MutationResult(
            boolean ok,
            GoalState goal,
            List<QueuedGoal> queue,
            Telemetry telemetry,
            String message,
            List<QueuedGoal> queued,
            QueuedGoal started,
            CompletedGoal completed,
            List<CompletedGoal> completedRun,
            QueuedGoal dequeued,
            QueuedGoal removed
    ) {
    }

    public record TemplateMetadata(
            String name,
            String path,
            String description,
            List<String> aliases,
            boolean allowCommands,
            List<String> requiredPlaceholders,
            List<String> requiredFlags,
            boolean requiresArgs
    ) {
    }

    public record TemplatesResult(boolean enabled, List<TemplateMetadata> templates, String message) {
        public static TemplatesResult enabled(List<TemplateMetadata> templates) {
            return new TemplatesResult(true, List.copyOf(templates), null);
        }

        public static TemplatesResult disabled(String message) {
            return new TemplatesResult(false, List.of(), message);
        }
    }
}
