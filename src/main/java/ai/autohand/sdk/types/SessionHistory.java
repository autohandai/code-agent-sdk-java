package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** Typed contracts for paginated persisted-session history. */
public final class SessionHistory {
    private SessionHistory() {
    }

    public record Params(
            @JsonInclude(JsonInclude.Include.NON_NULL) Integer page,
            @JsonInclude(JsonInclude.Include.NON_NULL) Integer pageSize) {
        public Params {
            if (page != null && page < 1) {
                throw new IllegalArgumentException("Session history page must be positive.");
            }
            if (pageSize != null && pageSize < 1) {
                throw new IllegalArgumentException("Session history page size must be positive.");
            }
        }

        public static Params defaults() {
            return new Params(null, null);
        }
    }

    public enum Status {
        ACTIVE("active"),
        COMPLETED("completed"),
        CRASHED("crashed");

        private final String cliValue;

        Status(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }

        @JsonCreator
        public static Status fromCliValue(String value) {
            for (Status status : values()) {
                if (status.cliValue.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown session history status: " + value);
        }
    }

    public record Entry(
            String sessionId,
            String createdAt,
            String lastActiveAt,
            String projectName,
            String model,
            int messageCount,
            Status status) {
    }

    public record Result(List<Entry> sessions, int currentPage, int totalPages, int totalItems) {
        public Result {
            sessions = sessions == null ? List.of() : List.copyOf(sessions);
        }
    }
}
