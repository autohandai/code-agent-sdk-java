package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** Typed contracts for accepting or rejecting a batch of proposed file changes. */
public final class ChangesDecision {
    private ChangesDecision() {
    }

    public enum Action {
        ACCEPT_ALL("accept_all"),
        REJECT_ALL("reject_all"),
        ACCEPT_SELECTED("accept_selected");

        private final String cliValue;

        Action(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }
    }

    public record Params(
            String batchId,
            Action action,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<String> selectedChangeIds) {
        public Params {
            if (batchId == null || batchId.isBlank()) {
                throw new IllegalArgumentException("A non-empty changes batch ID is required.");
            }
            if (action == null) {
                throw new IllegalArgumentException("A changes decision action is required.");
            }
            selectedChangeIds = selectedChangeIds == null ? null : List.copyOf(selectedChangeIds);
            if (action == Action.ACCEPT_SELECTED && (selectedChangeIds == null || selectedChangeIds.isEmpty())) {
                throw new IllegalArgumentException("accept_selected requires at least one change ID.");
            }
        }
    }

    public record Error(String changeId, String error) {
    }

    public record Result(boolean success, int appliedCount, int skippedCount, List<Error> errors) {
        public Result {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }
}
