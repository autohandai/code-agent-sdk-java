package ai.autohand.sdk.types;

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
}
