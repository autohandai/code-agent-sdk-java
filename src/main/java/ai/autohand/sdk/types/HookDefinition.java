package ai.autohand.sdk.types;

public record HookDefinition(
        String event,
        String command,
        String description,
        boolean enabled,
        int timeoutSeconds
) {
}
