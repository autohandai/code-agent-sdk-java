package ai.autohand.sdk.types;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A completed tool step, including results already persisted by the CLI. */
public record AgentStep(int stepNumber, String thought, List<ToolCall> toolCalls, List<ToolResult> toolResults) {
    public AgentStep {
        if (stepNumber < 1) throw new IllegalArgumentException("stepNumber must be positive");
        toolCalls = List.copyOf(toolCalls);
        toolResults = List.copyOf(toolResults);
    }

    public record ToolCall(String id, String tool, Map<String, Object> args) {
        public ToolCall {
            args = Collections.unmodifiableMap(new LinkedHashMap<>(args));
        }
    }

    public record ToolResult(String tool, boolean success, String output, String error) { }
}
