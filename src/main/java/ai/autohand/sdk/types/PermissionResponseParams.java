package ai.autohand.sdk.types;

public record PermissionResponseParams(
        String requestId,
        PermissionDecision decision,
        Boolean allowed,
        String alternative,
        String message
) {
}
