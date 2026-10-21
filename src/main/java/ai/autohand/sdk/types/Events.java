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

    public enum TokenUsageStatus {
        ACTUAL,
        UNAVAILABLE
    }

    public enum LearnProgressStatus {
        ANALYZING,
        LOADING_REGISTRY,
        EVALUATING,
        GENERATING,
        UPDATING
    }

    public enum FileChangeType {
        CREATE,
        MODIFY,
        DELETE
    }

    public enum HookSessionType {
        STARTUP,
        RESUME,
        CLEAR
    }

    public enum HookSessionEndReason {
        QUIT,
        CLEAR,
        EXIT,
        ERROR
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

    /** A tool boundary at which the host may request a resumable stop. */
    public record StepEndEvent(String stepId, AgentStep step, String timestamp) implements Event { }

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

        public FileChangeType fileChangeType() {
            return switch (changeType) {
                case "create" -> FileChangeType.CREATE;
                case "modify" -> FileChangeType.MODIFY;
                case "delete" -> FileChangeType.DELETE;
                default -> throw new IllegalStateException("Unsupported file change type: " + changeType);
            };
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
            double duration,
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

    public record HookPostResponseEvent(
            long tokensUsed,
            TokenUsageStatus tokensUsageStatus,
            int toolCallsCount,
            double duration,
            String timestamp
    ) implements Event {
    }

    public record HookSessionErrorEvent(
            String error,
            String code,
            Map<String, Object> context,
            String timestamp
    ) implements Event {
        public HookSessionErrorEvent {
            context = context == null
                    ? null
                    : Collections.unmodifiableMap(new LinkedHashMap<>(context));
        }
    }

    public record HookStopEvent(
            long tokensUsed,
            TokenUsageStatus tokensUsageStatus,
            int toolCallsCount,
            double duration,
            String timestamp
    ) implements Event {
    }

    public record HookSessionStartEvent(HookSessionType sessionType, String timestamp) implements Event {
    }

    public record HookSessionEndEvent(
            HookSessionEndReason reason,
            double duration,
            String timestamp
    ) implements Event {
    }

    public record HookSubagentStopEvent(
            String subagentId,
            String subagentName,
            String subagentType,
            boolean success,
            double duration,
            String error,
            String timestamp
    ) implements Event {
    }

    public record HookPermissionRequestEvent(
            String tool,
            String path,
            String command,
            Map<String, Object> args,
            String timestamp
    ) implements Event {
        public HookPermissionRequestEvent {
            args = args == null
                    ? null
                    : Collections.unmodifiableMap(new LinkedHashMap<>(args));
        }
    }

    public record HookNotificationEvent(
            String notificationType,
            String message,
            String timestamp
    ) implements Event {
    }

    public record HookContextCompactedEvent(
            long croppedCount,
            String summary,
            double usagePercent,
            String reason,
            String timestamp
    ) implements Event {
    }

    public record HookContextOverflowEvent(
            long tokensBefore,
            long tokensAfter,
            long croppedCount,
            double usagePercent,
            String timestamp
    ) implements Event {
    }

    public record HookContextWarningEvent(
            double usagePercent,
            long remainingTokens,
            String timestamp
    ) implements Event {
    }

    public record HookContextCriticalEvent(
            double usagePercent,
            long remainingTokens,
            String timestamp
    ) implements Event {
    }

    public record McpInvocationRequestEvent(
            String requestId,
            String toolName,
            Map<String, Object> args,
            String timestamp
    ) implements Event {
        public McpInvocationRequestEvent {
            args = args == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(args));
        }
    }

    public record McpTool(String name, String description, String serverName) {
    }

    public record McpToolsChangedEvent(List<McpTool> tools, String timestamp) implements Event {
        public McpToolsChangedEvent {
            tools = tools == null ? List.of() : List.copyOf(tools);
        }
    }

    public record LearnProgressEvent(LearnProgressStatus status, String timestamp) implements Event {
    }

    public record ErrorEvent(int code, String message, String timestamp) implements Event {
    }

    public record UnknownEvent(String method, JsonNode params, String timestamp) implements Event {
    }
}
