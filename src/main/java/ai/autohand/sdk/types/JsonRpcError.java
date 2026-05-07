package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 error object.
 *
 * @param code the error code
 * @param message the human-readable error description
 * @param data optional additional error data
 * @see JsonRpcErrorCode
 */
public record JsonRpcError(
    int code,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode data
) {
    @Override
    public String toString() {
        return "JsonRpcError{code=" + code + ", message='" + message + "'}";
    }
}
