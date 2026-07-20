package ai.autohand.sdk.types;

/** Typed contracts for resolving a directory-access prompt. */
public final class DirectoryAccessResponse {
    private DirectoryAccessResponse() {
    }

    public record Params(String requestId, boolean granted) {
        public Params {
            if (requestId == null || requestId.isBlank()) {
                throw new IllegalArgumentException("A non-empty directory-access request ID is required.");
            }
        }
    }

    public record Result(boolean success) {
    }
}
