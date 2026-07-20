package ai.autohand.sdk.types;

/** Typed contracts for changing automatic context compaction at runtime. */
public final class ContextCompaction {
    private ContextCompaction() {
    }

    public record Params(boolean enabled) {
    }

    public record Result(boolean enabled) {
    }
}
