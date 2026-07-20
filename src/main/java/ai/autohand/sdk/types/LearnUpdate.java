package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** Typed result for updating installed project-learning skills. */
public final class LearnUpdate {
    private LearnUpdate() {
    }

    public enum Status {
        UPDATED("updated"),
        UNCHANGED("unchanged"),
        FAILED("failed");

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
            throw new IllegalArgumentException("Unknown learning update status: " + value);
        }
    }

    public record Entry(String name, Status status) {
    }

    public record Result(
            boolean success,
            int updated,
            int unchanged,
            List<Entry> results,
            String error) {
        public Result {
            results = results == null ? List.of() : List.copyOf(results);
        }
    }
}
