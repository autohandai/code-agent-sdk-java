package ai.autohand.sdk.types;

/** Typed contracts for conversation lifecycle RPCs. */
public final class Conversation {
    private Conversation() {
    }

    /** Result returned after replacing the active conversation. */
    public record ResetResult(String sessionId) {
    }
}
