package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 request.
 *
 * @param jsonrpc the JSON-RPC version (default "2.0")
 * @param method the method name to invoke
 * @param params optional method parameters
 * @param id optional request ID for response correlation
 */
public record JsonRpcRequest(
    String jsonrpc,
    String method,
    @JsonInclude(JsonInclude.Include.NON_NULL) JsonNode params,
    @JsonInclude(JsonInclude.Include.NON_NULL) Object id
) {
    public JsonRpcRequest {
        if (jsonrpc == null) jsonrpc = "2.0";
    }

    public JsonRpcRequest(String method, JsonNode params, Object id) {
        this("2.0", method, params, id);
    }
}
