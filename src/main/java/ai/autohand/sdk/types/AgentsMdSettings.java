package ai.autohand.sdk.types;

public record AgentsMdSettings(
        boolean enabled,
        String path,
        boolean create,
        boolean autoUpdate,
        boolean includeInPrompt,
        boolean preferWorkspace,
        boolean failOnMissing
) {
}
