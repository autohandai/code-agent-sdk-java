package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonValue;

/** Typed contracts for generating a reusable skill from project learning. */
public final class LearnGeneration {
    private LearnGeneration() {
    }

    public enum Scope {
        PROJECT("project"),
        USER("user");

        private final String cliValue;

        Scope(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }
    }

    public record Params(Scope scope) {
        public Params {
            if (scope == null) {
                throw new IllegalArgumentException("A learning generation scope is required.");
            }
        }
    }

    public record Result(boolean success, String skillName, String skillPath, String error) {
    }
}
