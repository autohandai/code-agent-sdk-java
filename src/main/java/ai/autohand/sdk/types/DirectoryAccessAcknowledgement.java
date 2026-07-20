package ai.autohand.sdk.types;

/** Typed contracts for acknowledging receipt of a directory-access prompt. */
public final class DirectoryAccessAcknowledgement {
    private DirectoryAccessAcknowledgement() {
    }

    public record Params(String requestId) {
        public Params {
            if (requestId == null || requestId.isBlank()) {
                throw new IllegalArgumentException("A non-empty directory-access request ID is required.");
            }
        }
    }

    public record Result(boolean success) {
    }
}
