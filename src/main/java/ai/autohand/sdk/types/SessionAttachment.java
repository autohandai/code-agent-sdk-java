package ai.autohand.sdk.types;

/** Typed contracts for attaching the active RPC client to a persisted session. */
public final class SessionAttachment {
    private SessionAttachment() {
    }

    public record Params(String sessionId) {
        public Params {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("A non-empty session ID is required.");
            }
        }
    }

    public record Result(
            boolean success,
            String sessionId,
            String workspaceRoot,
            Integer messageCount,
            String error) {
    }
}
