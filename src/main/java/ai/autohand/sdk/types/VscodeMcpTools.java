package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/** Typed contracts for registering MCP tools hosted by a VS Code extension. */
public final class VscodeMcpTools {
    private VscodeMcpTools() {
    }

    public record InputSchema(
            String type,
            Map<String, Object> properties,
            @JsonInclude(JsonInclude.Include.NON_NULL) List<String> required) {
        public InputSchema {
            if (!"object".equals(type)) {
                throw new IllegalArgumentException("MCP input schema type must be object.");
            }
            properties = properties == null ? Map.of() : Map.copyOf(properties);
            required = required == null ? null : List.copyOf(required);
        }

        public static InputSchema object(Map<String, Object> properties, List<String> required) {
            return new InputSchema("object", properties, required);
        }
    }

    public record Tool(
            String name,
            String description,
            String serverName,
            @JsonInclude(JsonInclude.Include.NON_NULL) InputSchema inputSchema) {
        public Tool {
            if (name == null || name.isBlank() || serverName == null || serverName.isBlank()) {
                throw new IllegalArgumentException("MCP tool and server names must be non-empty.");
            }
        }
    }

    public record Params(List<Tool> tools) {
        public Params {
            tools = tools == null ? List.of() : List.copyOf(tools);
        }
    }

    public record Result(boolean success) {
    }
}
