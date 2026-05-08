package ai.autohand.sdk.rpc;

import ai.autohand.sdk.sdk.RequestTimeoutException;
import ai.autohand.sdk.sdk.RpcException;
import ai.autohand.sdk.sdk.TransportException;
import ai.autohand.sdk.transport.Transport;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.HookDefinition;
import ai.autohand.sdk.types.HookEvent;
import ai.autohand.sdk.types.McpServerConfig;
import ai.autohand.sdk.types.PermissionMode;
import ai.autohand.sdk.types.PermissionResponseParams;
import ai.autohand.sdk.types.PromptParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** JSON-RPC client for the Autohand CLI subprocess. */
public final class RPCClient {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Transport transport;
    private final AtomicLong nextId = new AtomicLong();
    private final Map<String, JsonNode> pendingResponses = new ConcurrentHashMap<>();
    private final ThreadLocal<Consumer<Event>> activeEventConsumer = new ThreadLocal<>();

    public RPCClient(Transport transport) {
        this.transport = transport;
    }

    public JsonNode request(String method, Object params) {
        return request(method, params, event -> {
        });
    }

    public JsonNode request(String method, Object params, Consumer<Event> onEvent) {
        if (!transport.isRunning()) {
            throw new TransportException("Autohand CLI process is not running. Call start() before sending RPC requests.");
        }

        Consumer<Event> previousConsumer = activeEventConsumer.get();
        if (previousConsumer == null && onEvent != null) {
            activeEventConsumer.set(onEvent);
        }

        try {
            long id = nextId.incrementAndGet();
            String idKey = Long.toString(id);
            ObjectNode request = MAPPER.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("method", method);
            request.put("id", id);
            request.set("params", params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params));

            try {
                transport.writeLine(MAPPER.writeValueAsString(request));
            } catch (JsonProcessingException e) {
                throw new TransportException("Failed to serialize JSON-RPC request: " + method, e);
            }

            long timeoutMs = transport.config().timeoutMs();
            long deadline = System.nanoTime() + Duration.ofMillis(timeoutMs).toNanos();

            while (true) {
                JsonNode pending = pendingResponses.remove(idKey);
                if (pending != null) {
                    return responseResult(method, pending);
                }

                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    throw new RequestTimeoutException(method, timeoutMs);
                }

                long waitMs = Math.max(1, Math.min(Duration.ofNanos(remainingNanos).toMillis(), 1_000));
                String line = transport.takeLine(Duration.ofMillis(waitMs));
                if (line == null) {
                    continue;
                }

                JsonNode message = parseLine(line);
                if (message == null) {
                    continue;
                }

                if (message.has("method") && !message.has("id")) {
                    Consumer<Event> target = activeEventConsumer.get();
                    (target == null ? onEvent : target).accept(toEvent(message));
                    continue;
                }

                if (message.has("id")) {
                    String responseId = idKey(message.get("id"));
                    if (idKey.equals(responseId)) {
                        return responseResult(method, message);
                    }
                    pendingResponses.put(responseId, message);
                }
            }
        } finally {
            if (previousConsumer == null) {
                activeEventConsumer.remove();
            } else {
                activeEventConsumer.set(previousConsumer);
            }
        }
    }

    public <T> T request(String method, Object params, Class<T> type) {
        return convert(request(method, params), type);
    }

    public void prompt(PromptParams params, Consumer<Event> onEvent) {
        request("autohand.prompt", params, onEvent);
    }

    public JsonNode abort(Map<String, Object> params) {
        return request("autohand.abort", params == null ? Map.of() : params);
    }

    public JsonNode getState() {
        return request("autohand.getState", Map.of());
    }

    public JsonNode getMessages() {
        return request("autohand.getMessages", Map.of());
    }

    public JsonNode permissionResponse(PermissionResponseParams params) {
        ObjectNode normalized = MAPPER.createObjectNode();
        normalized.put("requestId", params.requestId());
        if (params.decision() != null) {
            normalized.put("decision", params.decision().cliValue());
        }
        if (params.allowed() != null) {
            normalized.put("allowed", params.allowed());
        }
        if (params.alternative() != null) {
            normalized.put("alternative", params.alternative());
        }
        if (params.message() != null) {
            normalized.put("message", params.message());
        }
        return request("autohand.permissionResponse", normalized);
    }

    public JsonNode setPermissionMode(PermissionMode mode) {
        return request("autohand.permissionModeSet", Map.of("mode", mode.cliValue()));
    }

    public JsonNode setPlanMode(boolean enabled) {
        return request("autohand.planModeSet", Map.of("enabled", enabled));
    }

    public JsonNode setModel(String model) {
        return request("autohand.modelSet", Map.of("model", model == null ? "" : model));
    }

    public JsonNode setMaxThinkingTokens(Integer tokens) {
        ObjectNode params = MAPPER.createObjectNode();
        if (tokens == null) {
            params.putNull("maxThinkingTokens");
        } else {
            params.put("maxThinkingTokens", tokens);
        }
        return request("autohand.maxThinkingTokensSet", params);
    }

    public JsonNode applyFlagSettings(Map<String, Object> settings) {
        return request("autohand.applyFlagSettings", Map.of("settings", settings == null ? Map.of() : settings));
    }

    public JsonNode getSupportedModels() {
        return request("autohand.getSupportedModels", Map.of());
    }

    public JsonNode getSupportedCommands() {
        return request("autohand.getSupportedCommands", Map.of());
    }

    public JsonNode getContextUsage() {
        return request("autohand.getContextUsage", Map.of());
    }

    public JsonNode reloadPlugins() {
        return request("autohand.reloadPlugins", Map.of());
    }

    public JsonNode getAccountInfo() {
        return request("autohand.getAccountInfo", Map.of());
    }

    public JsonNode toggleMcpServer(String serverName, boolean enabled) {
        return request("autohand.mcp.toggleServer", Map.of("serverName", serverName, "enabled", enabled));
    }

    public JsonNode reconnectMcpServer(String serverName) {
        return request("autohand.mcp.reconnectServer", Map.of("serverName", serverName));
    }

    public JsonNode setMcpServers(Map<String, McpServerConfig> servers) {
        return request("autohand.mcp.setServers", Map.of("servers", servers == null ? Map.of() : servers));
    }

    public JsonNode saveSession() {
        return request("autohand.saveSession", Map.of());
    }

    public JsonNode resumeSession(String sessionId) {
        return request("autohand.resumeSession", Map.of("sessionId", sessionId));
    }

    public JsonNode getHooks() {
        return request("autohand.hooks.getHooks", Map.of());
    }

    public JsonNode addHook(HookDefinition hook) {
        return request("autohand.hooks.addHook", Map.of("hook", hook));
    }

    public JsonNode removeHook(HookEvent event, int index) {
        return request("autohand.hooks.removeHook", Map.of("event", event.toCliString(), "index", index));
    }

    public JsonNode toggleHook(HookEvent event, int index) {
        return request("autohand.hooks.toggleHook", Map.of("event", event.toCliString(), "index", index));
    }

    public Transport transport() {
        return transport;
    }

    public static <T> T convert(JsonNode node, Class<T> type) {
        if (type == JsonNode.class) {
            return type.cast(node);
        }
        try {
            return MAPPER.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new TransportException("Failed to map Autohand RPC result to " + type.getSimpleName() + ".", e);
        }
    }

    private JsonNode responseResult(String method, JsonNode response) {
        if (response.hasNonNull("error")) {
            JsonNode error = response.get("error");
            throw new RpcException(method, error.path("code").asInt(0), error.path("message").asText("Unknown RPC error"));
        }
        return response.has("result") ? response.get("result") : MAPPER.createObjectNode();
    }

    private JsonNode parseLine(String line) {
        try {
            return MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            if (transport.config().debug()) {
                System.err.println("[autohand-sdk] ignoring non-JSON stdout line: " + line);
            }
            return null;
        }
    }

    private Event toEvent(JsonNode message) {
        String method = message.path("method").asText("");
        JsonNode params = message.path("params");
        String timestamp = text(params, "timestamp", Instant.now().toString());

        return switch (method) {
            case "autohand.agentStart" -> new Events.AgentStartEvent(
                    text(params, "sessionId", "session_id", null),
                    text(params, "model", null),
                    text(params, "workspace", null),
                    timestamp);
            case "autohand.agentEnd" -> new Events.AgentEndEvent(
                    text(params, "sessionId", "session_id", null),
                    text(params, "reason", "completed"),
                    timestamp);
            case "autohand.turnStart" -> new Events.TurnStartEvent(
                    text(params, "turnId", "turn_id", null),
                    text(params, "sessionId", "session_id", null),
                    timestamp);
            case "autohand.turnEnd" -> new Events.TurnEndEvent(
                    text(params, "turnId", "turn_id", null),
                    text(params, "status", "completed"),
                    timestamp);
            case "autohand.messageStart" -> new Events.MessageStartEvent(
                    text(params, "messageId", "message_id", null),
                    text(params, "role", "assistant"),
                    timestamp);
            case "autohand.messageUpdate" -> new Events.MessageUpdateEvent(
                    text(params, "messageId", "message_id", null),
                    text(params, "delta", ""),
                    timestamp);
            case "autohand.messageEnd" -> new Events.MessageEndEvent(
                    text(params, "messageId", "message_id", null),
                    text(params, "content", ""),
                    timestamp);
            case "autohand.toolStart" -> new Events.ToolStartEvent(
                    text(params, "toolName", "tool_name", "tool"),
                    text(params, "toolCallId", "tool_call_id", "toolId", "tool_id", null),
                    timestamp);
            case "autohand.toolUpdate" -> new Events.ToolUpdateEvent(
                    text(params, "toolName", "tool_name", "tool"),
                    text(params, "toolCallId", "tool_call_id", "toolId", "tool_id", null),
                    text(params, "output", "delta", ""),
                    timestamp);
            case "autohand.toolEnd" -> new Events.ToolEndEvent(
                    text(params, "toolName", "tool_name", "tool"),
                    text(params, "toolCallId", "tool_call_id", "toolId", "tool_id", null),
                    params.path("success").asBoolean(!params.has("error")),
                    text(params, "output", "error", ""),
                    timestamp);
            case "autohand.permissionRequest" -> new Events.PermissionRequestEvent(
                    text(params, "requestId", "request_id", null),
                    text(params, "tool", "toolName", "tool_name", null),
                    text(params, "description", "message", ""),
                    timestamp);
            case "autohand.hook.fileModified" -> new Events.FileModifiedEvent(
                    text(params, "filePath", "file_path", "path", null),
                    text(params, "changeType", "change_type", "modify"),
                    text(params, "toolCallId", "tool_call_id", "toolId", "tool_id", null),
                    timestamp);
            case "autohand.error" -> new Events.ErrorEvent(
                    params.path("code").asInt(0),
                    text(params, "message", "Unknown Autohand error"),
                    timestamp);
            default -> new Events.UnknownEvent(method, params, timestamp);
        };
    }

    private static String idKey(JsonNode node) {
        return node.isTextual() ? node.asText() : Long.toString(node.asLong());
    }

    private static String text(JsonNode node, String firstKey, String defaultValue) {
        return text(node, new String[]{firstKey}, defaultValue);
    }

    private static String text(JsonNode node, String firstKey, String secondKey, String defaultValue) {
        return text(node, new String[]{firstKey, secondKey}, defaultValue);
    }

    private static String text(JsonNode node, String firstKey, String secondKey, String thirdKey, String defaultValue) {
        return text(node, new String[]{firstKey, secondKey, thirdKey}, defaultValue);
    }

    private static String text(JsonNode node, String firstKey, String secondKey, String thirdKey, String fourthKey,
                               String defaultValue) {
        return text(node, new String[]{firstKey, secondKey, thirdKey, fourthKey}, defaultValue);
    }

    private static String text(JsonNode node, String firstKey, String secondKey, String thirdKey, String fourthKey,
                               String fifthKey, String defaultValue) {
        return text(node, new String[]{firstKey, secondKey, thirdKey, fourthKey, fifthKey}, defaultValue);
    }

    private static String text(JsonNode node, String[] keys, String defaultValue) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return defaultValue;
    }
}
