package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Information about an available subagent.
 */
public record AgentInfo(
    String id,
    String name,
    String description,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> tools,
    String model,
    String source,
    String extensionId,
    String extensionVersion,
    String extensionScope
) {
    public AgentInfo(String id, String name, String description, List<String> tools) {
        this(id, name, description, tools, null, null, null, null, null);
    }

    public AgentInfo {
        tools = tools == null ? null : List.copyOf(tools);
    }

    /** Effective session registry returned by autohand.getSupportedAgents. */
    public record Result(List<AgentInfo> agents) {
        public Result {
            agents = List.copyOf(agents);
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Result fromRpc(JsonNode value) {
            if (!value.path("agents").isArray()) {
                throw new IllegalArgumentException("Agent discovery result must contain an agents array");
            }
            List<AgentInfo> agents = new ArrayList<>();
            for (JsonNode entry : value.path("agents")) {
                if (!entry.path("tools").isArray()) {
                    throw new IllegalArgumentException("Agent tools must be an array");
                }
                List<String> tools = new ArrayList<>();
                for (JsonNode tool : entry.path("tools")) {
                    if (!tool.isTextual()) throw new IllegalArgumentException("Agent tool must be a string");
                    tools.add(tool.textValue());
                }
                String scope = string(entry, "extensionScope", false);
                if (scope != null && !scope.equals("user") && !scope.equals("project")) {
                    throw new IllegalArgumentException("Agent extensionScope must be user or project");
                }
                agents.add(new AgentInfo(string(entry, "id", true), string(entry, "name", true),
                        string(entry, "description", true), tools, string(entry, "model", false),
                        string(entry, "source", false), string(entry, "extensionId", false),
                        string(entry, "extensionVersion", false), scope));
            }
            return new Result(agents);
        }

        private static String string(JsonNode value, String key, boolean required) {
            JsonNode field = value.path(key);
            if (!required && (field.isMissingNode() || field.isNull())) return null;
            if (!field.isTextual()) throw new IllegalArgumentException("Agent " + key + " must be a string");
            return field.textValue();
        }
    }
}
