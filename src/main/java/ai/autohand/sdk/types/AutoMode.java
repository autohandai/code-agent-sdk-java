package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** Typed contracts for autonomous CLI runs. */
public final class AutoMode {
    private AutoMode() {
    }

    public record StartParams(
            String prompt,
            Integer maxIterations,
            String completionPromise,
            Boolean useWorktree,
            Integer checkpointInterval,
            Integer maxRuntime,
            Double maxCost) {
    }

    public record StartResult(boolean success, String sessionId, String error) {
    }

    public enum Status {
        RUNNING("running"),
        PAUSED("paused"),
        COMPLETED("completed"),
        CANCELLED("cancelled"),
        FAILED("failed");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static Status fromValue(String value) {
            for (Status status : values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown auto-mode status: " + value);
        }
    }

    public record Checkpoint(String commit, String message, String timestamp) {
    }

    public record State(
            String sessionId,
            Status status,
            int currentIteration,
            int maxIterations,
            int filesCreated,
            int filesModified,
            String branch,
            Checkpoint lastCheckpoint) {
    }

    public record StatusResult(boolean active, boolean paused, State state) {
    }
}
