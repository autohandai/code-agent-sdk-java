package ai.autohand.sdk.types;

/** Typed contracts for acknowledging receipt of a permission prompt. */
public final class PermissionAcknowledgement {
    private PermissionAcknowledgement() {
    }

    public record Params(String requestId) {
        public Params {
            if (requestId == null || requestId.isBlank()) {
                throw new IllegalArgumentException("A non-empty permission request ID is required.");
            }
        }
    }

    public record Result(boolean success) {
    }
}
