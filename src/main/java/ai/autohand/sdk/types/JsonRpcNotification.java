package ai.autohand.sdk.types;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * JSON-RPC 2.0 notification (server-to-client, no id).
 *
 * @param jsonrpc the JSON-RPC version
 * @param method the notification method name
 * @param params the notification payload
 */
public record JsonRpcNotification(
    String jsonrpc,
    String method,
    JsonNode params
) {
}
