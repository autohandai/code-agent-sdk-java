package ai.autohand.sdk.types;

import com.fasterxml.jackson.databind.JsonNode;

/** Concrete event records emitted by the SDK. */
public final class Events {
    private Events() {
    }

    public record AgentStartEvent(String sessionId, String model, String workspace, String timestamp) implements Event {
    }

    public record AgentEndEvent(String sessionId, String reason, String timestamp) implements Event {
    }

    public record TurnStartEvent(String turnId, String sessionId, String timestamp) implements Event {
    }

    public record TurnEndEvent(String turnId, String status, String timestamp) implements Event {
    }

    public record MessageStartEvent(String messageId, String role, String timestamp) implements Event {
    }

    public record MessageUpdateEvent(String messageId, String delta, String timestamp) implements Event {
    }

    public record MessageEndEvent(String messageId, String content, String timestamp) implements Event {
    }

    public record ToolStartEvent(String toolName, String toolCallId, String timestamp) implements Event {
        public String toolId() {
            return toolCallId;
        }
    }

    public record ToolUpdateEvent(String toolName, String toolCallId, String output, String timestamp) implements Event {
        public String toolId() {
            return toolCallId;
        }
    }

    public record ToolEndEvent(String toolName, String toolCallId, boolean success, String output, String timestamp) implements Event {
        public String toolId() {
            return toolCallId;
        }
    }

    public record PermissionRequestEvent(String requestId, String tool, String description, String timestamp) implements Event {
    }

    public record FileModifiedEvent(String filePath, String changeType, String toolCallId, String timestamp) implements Event {
        public String toolId() {
            return toolCallId;
        }
    }

    public record ErrorEvent(int code, String message, String timestamp) implements Event {
    }

    public record UnknownEvent(String method, JsonNode params, String timestamp) implements Event {
    }
}
