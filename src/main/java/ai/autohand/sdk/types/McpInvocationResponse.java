package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Typed client response to a CLI-issued VS Code MCP invocation request. */
public final class McpInvocationResponse {
    private McpInvocationResponse() {
    }

    public record Params(
            String requestId,
            boolean success,
            @JsonInclude(JsonInclude.Include.NON_NULL) String result,
            @JsonInclude(JsonInclude.Include.NON_NULL) String error) {
        public Params {
            if (requestId == null || requestId.isBlank()) {
                throw new IllegalArgumentException("A non-empty MCP invocation request ID is required.");
            }
            if (success && error != null) {
                throw new IllegalArgumentException("A successful MCP invocation response cannot include an error.");
            }
            if (!success && (error == null || error.isBlank())) {
                throw new IllegalArgumentException("A failed MCP invocation response requires an error.");
            }
        }
    }

    public record Result(boolean success) {
    }
}
