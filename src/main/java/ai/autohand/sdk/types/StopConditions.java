package ai.autohand.sdk.types;

import java.util.concurrent.CompletableFuture;

/** Common conditions; multiple conditions are combined with OR. */
public final class StopConditions {
    private StopConditions() { }

    public static StopCondition isStepCount(int count) {
        if (count < 1) throw new IllegalArgumentException("Step count must be positive");
        return context -> CompletableFuture.completedFuture(context.steps().size() >= count);
    }

    public static StopCondition hasToolCall(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Tool name must not be blank");
        String tool = name.trim();
        return context -> CompletableFuture.completedFuture(!context.steps().isEmpty()
                && context.steps().getLast().toolCalls().stream().anyMatch(call -> tool.equals(call.tool())));
    }
}
