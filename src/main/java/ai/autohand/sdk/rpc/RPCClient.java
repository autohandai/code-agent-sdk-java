package ai.autohand.sdk.rpc;

import ai.autohand.sdk.sdk.RequestTimeoutException;
import ai.autohand.sdk.sdk.RpcException;
import ai.autohand.sdk.sdk.TransportException;
import ai.autohand.sdk.transport.Transport;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.Autoresearch;
import ai.autohand.sdk.types.BrowserHandoff;
import ai.autohand.sdk.types.CommunitySkills;
import ai.autohand.sdk.types.Conversation;
import ai.autohand.sdk.types.Goals;
import ai.autohand.sdk.types.HookDefinition;
import ai.autohand.sdk.types.HookEvent;
import ai.autohand.sdk.types.McpServerConfig;
import ai.autohand.sdk.types.McpDiscovery;
import ai.autohand.sdk.types.PermissionMode;
import ai.autohand.sdk.types.PermissionResponseParams;
import ai.autohand.sdk.types.PromptParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/** JSON-RPC client for the Autohand CLI subprocess. */
public final class RPCClient {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final Executor EVENT_EXECUTOR = command ->
            Thread.ofVirtual().name("autohand-rpc-event").start(command);

    private final Transport transport;
    private final AtomicLong nextId = new AtomicLong();
    private final Map<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicReference<EventContext> activeEventContext = new AtomicReference<>();
    private final AtomicBoolean readerStarted = new AtomicBoolean();
    private final ReentrantLock promptLock = new ReentrantLock(true);
    private volatile RuntimeException readerFailure;

    public RPCClient(Transport transport) {
        this.transport = transport;
    }

    public JsonNode request(String method, Object params) {
        return requestInternal(method, params);
    }

    public JsonNode request(String method, Object params, Consumer<Event> onEvent) {
        if (onEvent == null) {
            return requestInternal(method, params);
        }

        promptLock.lock();
        EventContext context = new EventContext(onEvent);
        try {
            if (!activeEventContext.compareAndSet(null, context)) {
                throw new IllegalStateException("Another event-bearing RPC request is already active.");
            }
            try {
                JsonNode result = requestInternal(method, params);
                context.await(transport.config().timeoutMs());
                return result;
            } finally {
                activeEventContext.compareAndSet(context, null);
            }
        } finally {
            promptLock.unlock();
        }
    }

    private JsonNode requestInternal(String method, Object params) {
        if (!transport.isRunning()) {
            throw new TransportException("Autohand CLI process is not running. Call start() before sending RPC requests.");
        }
        RuntimeException failure = readerFailure;
        if (failure != null) {
            throw failure;
        }
        ensureReaderStarted();

        long id = nextId.incrementAndGet();
        String idKey = Long.toString(id);
        CompletableFuture<JsonNode> response = new CompletableFuture<>();
        pendingRequests.put(idKey, response);
        RuntimeException failureAfterRegistration = readerFailure;
        if (failureAfterRegistration != null) {
            response.completeExceptionally(failureAfterRegistration);
        }
        try {
            ObjectNode request = MAPPER.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("method", method);
            request.put("id", id);
            request.set("params", params == null ? MAPPER.createObjectNode() : MAPPER.valueToTree(params));

            try {
                transport.writeLine(MAPPER.writeValueAsString(request));
            } catch (JsonProcessingException exception) {
                throw new TransportException("Failed to serialize JSON-RPC request: " + method, exception);
            }

            long timeoutMs = transport.config().timeoutMs();
            try {
                return responseResult(method, response.get(timeoutMs, TimeUnit.MILLISECONDS));
            } catch (TimeoutException exception) {
                throw new RequestTimeoutException(method, timeoutMs);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new TransportException("Interrupted while waiting for Autohand RPC response: " + method,
                        exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new TransportException("Failed while waiting for Autohand RPC response: " + method, cause);
            }
        } finally {
            pendingRequests.remove(idKey, response);
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

    public Conversation.ResetResult reset() {
        return request("autohand.reset", Map.of(), Conversation.ResetResult.class);
    }

    public BrowserHandoff.CreateResult createBrowserHandoff(BrowserHandoff.CreateParams params) {
        return request("autohand.browserHandoff.create",
                params == null ? BrowserHandoff.CreateParams.defaults() : params,
                BrowserHandoff.CreateResult.class);
    }

    public BrowserHandoff.AttachResult attachBrowserHandoff(BrowserHandoff.AttachParams params) {
        return request("autohand.browserHandoff.attach", params, BrowserHandoff.AttachResult.class);
    }

    public CommunitySkills.RegistryResult getSkillsRegistry(CommunitySkills.RegistryParams params) {
        return request("autohand.getSkillsRegistry", params == null ? CommunitySkills.RegistryParams.cached() : params,
                CommunitySkills.RegistryResult.class);
    }

    public CommunitySkills.InstallResult installSkill(CommunitySkills.InstallParams params) {
        return request("autohand.installSkill", params, CommunitySkills.InstallResult.class);
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

    public Goals.SnapshotResult getGoal() {
        JsonNode result = request("autohand.goal.get", Map.of());
        if (featureDisabled(result)) {
            return Goals.SnapshotResult.disabled(result.path("message").asText("Persistent goals are disabled."));
        }
        return Goals.SnapshotResult.enabled(convert(result, Goals.Snapshot.class));
    }

    public Goals.MutationResult createGoal(Goals.CreateParams params) {
        return request("autohand.goal.create", goalParams(params), Goals.MutationResult.class);
    }

    public Goals.MutationResult updateGoal(Goals.UpdateParams params) {
        return request("autohand.goal.update", goalParams(params), Goals.MutationResult.class);
    }

    public Goals.MutationResult clearGoal() {
        return request("autohand.goal.clear", Map.of(), Goals.MutationResult.class);
    }

    public Goals.MutationResult queueGoal(Goals.CreateParams params) {
        return request("autohand.goal.queue", goalParams(params), Goals.MutationResult.class);
    }

    public Goals.MutationResult startQueuedGoal() {
        return request("autohand.goal.startQueued", Map.of(), Goals.MutationResult.class);
    }

    public Goals.TemplatesResult listGoalTemplates() {
        JsonNode result = request("autohand.goal.listTemplates", Map.of());
        if (featureDisabled(result)) {
            return Goals.TemplatesResult.disabled(result.path("message").asText("Persistent goals are disabled."));
        }
        List<Goals.TemplateMetadata> templates = MAPPER.convertValue(result,
                new TypeReference<List<Goals.TemplateMetadata>>() { });
        return Goals.TemplatesResult.enabled(templates);
    }

    public Autoresearch.StartResult startAutoresearch(Autoresearch.StartParams params) {
        return request("autohand.autoresearch.start", params, Autoresearch.StartResult.class);
    }

    public Autoresearch.StatusResult getAutoresearchStatus() {
        return request("autohand.autoresearch.status", Map.of(), Autoresearch.StatusResult.class);
    }

    public Autoresearch.StopResult stopAutoresearch() {
        return request("autohand.autoresearch.stop", Map.of(), Autoresearch.StopResult.class);
    }

    public Autoresearch.HistoryResult getAutoresearchHistory() {
        return request("autohand.autoresearch.history", Map.of(), Autoresearch.HistoryResult.class);
    }

    public Autoresearch.ReplayResult replayAutoresearch(Autoresearch.ReplayParams params) {
        return request("autohand.autoresearch.replay", params, Autoresearch.ReplayResult.class);
    }

    public Autoresearch.RescoreResult rescoreAutoresearch(Autoresearch.RescoreParams params) {
        return request("autohand.autoresearch.rescore", params, Autoresearch.RescoreResult.class);
    }

    public Autoresearch.CompareResult compareAutoresearch(Autoresearch.CompareParams params) {
        return request("autohand.autoresearch.compare", params, Autoresearch.CompareResult.class);
    }

    public Autoresearch.ParetoResult getAutoresearchPareto() {
        return request("autohand.autoresearch.pareto", Map.of(), Autoresearch.ParetoResult.class);
    }

    public Autoresearch.PinResult pinAutoresearch(Autoresearch.PinParams params) {
        return request("autohand.autoresearch.pin", params, Autoresearch.PinResult.class);
    }

    public Autoresearch.PruneResult pruneAutoresearch(Autoresearch.PruneParams params) {
        return request("autohand.autoresearch.prune", params == null ? new Autoresearch.PruneParams(null, null) : params,
                Autoresearch.PruneResult.class);
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

    public McpDiscovery.ListServersResult listMcpServers() {
        return request("autohand.mcp.listServers", Map.of(), McpDiscovery.ListServersResult.class);
    }

    public McpDiscovery.ListToolsResult listMcpTools(McpDiscovery.ListToolsParams params) {
        return request("autohand.mcp.listTools", params == null ? McpDiscovery.ListToolsParams.allServers() : params,
                McpDiscovery.ListToolsResult.class);
    }

    public McpDiscovery.GetServerConfigsResult getMcpServerConfigs() {
        return request("autohand.mcp.getServerConfigs", Map.of(), McpDiscovery.GetServerConfigsResult.class);
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

    private static ObjectNode goalParams(Goals.CreateParams params) {
        if (params == null || params.objective() == null || params.objective().isBlank()) {
            throw new IllegalArgumentException("A non-empty goal objective is required.");
        }
        ObjectNode node = MAPPER.createObjectNode().put("objective", params.objective());
        Goals.Budget budget = params.budget() == null ? Goals.Budget.none() : params.budget();
        put(node, "token_budget", budget.tokenBudget());
        put(node, "time_budget_seconds", budget.timeBudgetSeconds());
        put(node, "min_tokens_before_wrap_up", budget.minTokensBeforeWrapUp());
        put(node, "min_time_seconds_before_wrap_up", budget.minTimeSecondsBeforeWrapUp());
        return node;
    }

    private static ObjectNode goalParams(Goals.UpdateParams params) {
        if (params == null) {
            throw new IllegalArgumentException("Goal update parameters are required.");
        }
        ObjectNode node = MAPPER.createObjectNode();
        if (params.objective() != null) {
            node.put("objective", params.objective());
        }
        if (params.status() != null) {
            node.put("status", params.status().cliValue());
        }
        putUpdate(node, "token_budget", params.tokenBudget());
        putUpdate(node, "time_budget_seconds", params.timeBudgetSeconds());
        putUpdate(node, "min_tokens_before_wrap_up", params.minTokensBeforeWrapUp());
        putUpdate(node, "min_time_seconds_before_wrap_up", params.minTimeSecondsBeforeWrapUp());
        return node;
    }

    private static void put(ObjectNode node, String name, Long value) {
        if (value != null) {
            node.put(name, value);
        }
    }

    private static void putUpdate(ObjectNode node, String name, Goals.NullableUpdate<Long> update) {
        switch (update.action()) {
            case UNCHANGED -> { }
            case CLEAR -> node.putNull(name);
            case SET -> node.put(name, update.value());
        }
    }

    private static boolean featureDisabled(JsonNode result) {
        return result.isObject() && result.has("ok") && !result.path("ok").asBoolean(true)
                && result.hasNonNull("message");
    }

    private void ensureReaderStarted() {
        if (readerStarted.compareAndSet(false, true)) {
            long readerGeneration = transport.generation();
            Thread.ofVirtual().name("autohand-rpc-dispatcher")
                    .start(() -> readLoop(readerGeneration));
        }
    }

    private void readLoop(long readerGeneration) {
        try {
            while (true) {
                String line = transport.takeLine(Duration.ofSeconds(1), readerGeneration);
                if (line == null) {
                    continue;
                }

                JsonNode message = parseLine(line);
                if (message == null) {
                    continue;
                }

                if (message.has("method") && !message.has("id")) {
                    EventContext context = activeEventContext.get();
                    if (context != null) {
                        context.dispatch(toEvent(message));
                    }
                    continue;
                }

                if (message.has("id")) {
                    CompletableFuture<JsonNode> response = pendingRequests.get(idKey(message.get("id")));
                    if (response != null) {
                        response.complete(message);
                    }
                }
            }
        } catch (RuntimeException failure) {
            readerFailure = failure;
            pendingRequests.forEach((id, response) -> {
                if (pendingRequests.remove(id, response)) {
                    response.completeExceptionally(failure);
                }
            });
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
                    params.hasNonNull("tokensUsed") ? params.get("tokensUsed").asLong() : null,
                    text(params, "tokensUsageStatus", null),
                    params.hasNonNull("durationMs") ? params.get("durationMs").asLong() : null,
                    params.hasNonNull("contextPercent") ? params.get("contextPercent").asDouble() : null,
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
            case "autohand.autoresearch.start" -> autoresearchLifecycle("start", params, timestamp);
            case "autohand.autoresearch.status" -> autoresearchLifecycle("status", params, timestamp);
            case "autohand.autoresearch.pause" -> autoresearchLifecycle("pause", params, timestamp);
            case "autohand.autoresearch.event" -> new Events.AutoresearchOperationEvent(
                    text(params, "operation", null),
                    text(params, "phase", null),
                    text(params, "attemptId", "attempt_id", null),
                    params.path("success").asBoolean(false),
                    params.hasNonNull("applied") ? params.get("applied").asBoolean() : null,
                    text(params, "error", null),
                    timestamp);
            case "autohand.error" -> new Events.ErrorEvent(
                    params.path("code").asInt(0),
                    text(params, "message", "Unknown Autohand error"),
                    timestamp);
            default -> new Events.UnknownEvent(method, params, timestamp);
        };
    }

    private static Events.AutoresearchLifecycleEvent autoresearchLifecycle(
            String phase, JsonNode params, String timestamp) {
        return new Events.AutoresearchLifecycleEvent(
                phase,
                params.path("active").asBoolean(false),
                text(params, "goal", null),
                params.hasNonNull("iteration") ? params.get("iteration").asInt() : null,
                params.hasNonNull("maxIterations") ? params.get("maxIterations").asInt() : null,
                params.path("runsLogged").asInt(0),
                text(params, "statusText", "status_text", ""),
                text(params, "subcommand", null),
                text(params, "message", null),
                timestamp);
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

    private static final class EventContext {
        private final Consumer<Event> consumer;
        private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

        private EventContext(Consumer<Event> consumer) {
            this.consumer = consumer;
        }

        private synchronized void dispatch(Event event) {
            tail = tail.thenRunAsync(() -> consumer.accept(event), EVENT_EXECUTOR);
        }

        private void await(long timeoutMs) {
            CompletableFuture<Void> snapshot;
            synchronized (this) {
                snapshot = tail;
            }
            try {
                snapshot.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException exception) {
                throw new RequestTimeoutException("event callbacks", timeoutMs);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new TransportException("Interrupted while delivering Autohand RPC events.", exception);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new TransportException("Autohand event callback failed.", cause);
            }
        }
    }
}
