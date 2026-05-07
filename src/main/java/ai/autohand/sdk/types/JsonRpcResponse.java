package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 response.
 *
 * @param jsonrpc the JSON-RPC version
 * @param result the result payload, or {@code null} on error
 * @param error the error object, or {@code null} on success
 * @param id the request ID for correlation
 */
public record JsonRpcResponse(
    String jsonrpc,
    @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode result,
    @JsonInclude(JsonInclude.Include.NON_NULL) JsonRpcError error,
    Object id
) {
}
