package ai.autohand.sdk.types;

import java.util.List;
import java.util.Objects;

/** Discriminated success/failure result for loading a persisted session. */
public final class SessionDetails {
    private SessionDetails() {
    }

    public record Params(String sessionId) {
        public Params {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("A non-empty session ID is required.");
            }
        }
    }

    public sealed interface Result permits Success, Failure {
        boolean success();
    }

    public record Success(
            boolean success,
            String sessionId,
            String projectName,
            String model,
            int messageCount,
            String status,
            String createdAt,
            String lastActiveAt,
            String summary,
            List<RpcMessage> messages,
            String workspaceRoot) implements Result {
        public Success {
            if (!success) {
                throw new IllegalArgumentException("A successful session result must set success=true.");
            }
            Objects.requireNonNull(sessionId, "sessionId");
            Objects.requireNonNull(projectName, "projectName");
            Objects.requireNonNull(model, "model");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(createdAt, "createdAt");
            Objects.requireNonNull(lastActiveAt, "lastActiveAt");
            Objects.requireNonNull(workspaceRoot, "workspaceRoot");
            messages = messages == null ? List.of() : List.copyOf(messages);
        }
    }

    public record Failure(boolean success, String error) implements Result {
        public Failure {
            if (success) {
                throw new IllegalArgumentException("A failed session result must set success=false.");
            }
            error = error == null || error.isBlank() ? "Session could not be loaded." : error;
        }
    }
}
