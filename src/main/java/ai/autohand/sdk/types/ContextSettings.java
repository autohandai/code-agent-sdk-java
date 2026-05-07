package ai.autohand.sdk.types;

public record ContextSettings(
        boolean compact,
        int maxTokens,
        double compressionThreshold,
        double summarizationThreshold,
        boolean includeMemory
) {
}
