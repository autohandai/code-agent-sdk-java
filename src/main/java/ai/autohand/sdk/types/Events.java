package ai.autohand.sdk.types;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

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

    public record TurnEndEvent(
            String turnId,
            String status,
            Long tokensUsed,
            String tokensUsageStatus,
            Long durationMs,
            Double contextPercent,
            String timestamp
    ) implements Event {
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

    /** Lifecycle notification emitted when autoresearch starts, reports status, or pauses. */
    public record AutoresearchLifecycleEvent(
            String phase,
            boolean active,
            String goal,
            Integer iteration,
            Integer maxIterations,
            int runsLogged,
            String statusText,
            String subcommand,
            String message,
            String timestamp
    ) implements Event {
    }

    /** Notification emitted around replayable-ledger operations. */
    public record AutoresearchOperationEvent(
            String operation,
            String phase,
            String attemptId,
            boolean success,
            Boolean applied,
            String error,
            String timestamp
    ) implements Event {
    }

    public record AutoModeIterationEvent(
            String sessionId,
            int iteration,
            List<String> actions,
            Long tokensUsed,
            String timestamp
    ) implements Event {
        public AutoModeIterationEvent {
            actions = actions == null ? List.of() : List.copyOf(actions);
        }
    }

    public record AutoModeCompleteEvent(
            String sessionId,
            int iterations,
            int filesCreated,
            int filesModified,
            String timestamp
    ) implements Event {
    }

    public record AutoModeErrorEvent(String sessionId, String error, String timestamp) implements Event {
    }

    public record HookPreToolEvent(
            String toolId,
            String toolName,
            Map<String, Object> args,
            String timestamp
    ) implements Event {
        public HookPreToolEvent {
            args = args == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(args));
        }
    }

    public record HookPostToolEvent(
            String toolId,
            String toolName,
            boolean success,
            long duration,
            String output,
            String timestamp
    ) implements Event {
    }

    public record HookPrePromptEvent(
            String instruction,
            List<String> mentionedFiles,
            String timestamp
    ) implements Event {
        public HookPrePromptEvent {
            mentionedFiles = mentionedFiles == null ? List.of() : List.copyOf(mentionedFiles);
        }
    }

    public record ErrorEvent(int code, String message, String timestamp) implements Event {
    }

    public record UnknownEvent(String method, JsonNode params, String timestamp) implements Event {
    }
}
