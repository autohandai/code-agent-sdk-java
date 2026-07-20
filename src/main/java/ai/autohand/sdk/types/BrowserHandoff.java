package ai.autohand.sdk.types;

/** Typed contracts for browser-to-CLI session handoff. */
public final class BrowserHandoff {
    private BrowserHandoff() {
    }

    public record CreateParams(String extensionId, String installUrl) {
        public static CreateParams defaults() {
            return new CreateParams(null, null);
        }
    }

    public record CreateResult(
            String token,
            String sessionId,
            String workspaceRoot,
            String createdAt,
            String expiresAt,
            String url) {
    }

    public record AttachParams(String token) {
    }

    public record AttachResult(
            boolean success,
            String sessionId,
            String workspaceRoot,
            Integer messageCount) {
    }
}
