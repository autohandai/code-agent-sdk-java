package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * A message in the conversation history.
 */
public record RpcMessage(
    String id,
    String role,
    String content,
    String timestamp,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<ToolCall> toolCalls
) {
    public record ToolCall(
        String id,
        String name,
        Map<String, Object> args
    ) {
    }
}
