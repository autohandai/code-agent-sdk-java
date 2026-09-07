package ai.autohand.sdk.rpc;

import ai.autohand.sdk.sdk.RequestTimeoutException;
import ai.autohand.sdk.sdk.RpcException;
import ai.autohand.sdk.sdk.TransportException;
import ai.autohand.sdk.transport.Transport;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.AgentStep;
import ai.autohand.sdk.types.Autoresearch;
import ai.autohand.sdk.types.AutoMode;
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
import java.util.Objects;
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
    static final Executor EVENT_EXECUTOR = command ->
            Thread.ofVirtual().name("autohand-rpc-event").start(command);

    private final Transport transport;
    private final AtomicLong nextId = new AtomicLong();
    private final Map<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final AtomicReference<EventContext> activeEventContext = new AtomicReference<>();
    private final AtomicReference<PromptTurn> activePrompt = new AtomicReference<>();
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
        return requestInternal(method, params, transport.config().timeoutMs(), null, null);
    }

    private JsonNode requestInternal(String method, Object params, long timeoutMs,
                                     PromptTurn decisionGuard, CompletableFuture<?> failureSignal) {
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
                String line = MAPPER.writeValueAsString(request);
                if (decisionGuard == null) transport.writeLine(line);
                else synchronized (decisionGuard) {
                    if (activePrompt.get() != decisionGuard
                            || (!method.equals("autohand.abort") && !decisionGuard.active())) return null;
                    transport.writeLine(line);
                }
            } catch (JsonProcessingException exception) {
                throw new TransportException("Failed to serialize JSON-RPC request: " + method, exception);
            }

            try {
                var completion = failureSignal == null ? response : CompletableFuture.anyOf(response, failureSignal);
                return responseResult(method, (JsonNode) completion.get(timeoutMs, TimeUnit.MILLISECONDS));
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

    public JsonNode prompt(PromptParams params) {
        return prompt(params, null, new AtomicBoolean());
    }

    public void prompt(PromptParams params, Consumer<Event> onEvent) {
        prompt(params, onEvent, new AtomicBoolean());
    }

    public JsonNode prompt(PromptParams params, Consumer<Event> onEvent, AtomicBoolean cancellation) {
        Objects.requireNonNull(params, "prompt params");
        Objects.requireNonNull(cancellation, "prompt cancellation");
        promptLock.lock();
        var turn = new PromptTurn(this, onEvent == null ? event -> { } : onEvent, params.stopWhen(), cancellation);
        try {
            activePrompt.set(turn);
            ObjectNode wire = MAPPER.createObjectNode().put("message", params.message());
            if (!params.stopWhen().isEmpty()) wire.putObject("stopWhen").put("mode", "host");
            JsonNode result = requestInternal("autohand.prompt", wire, transport.config().timeoutMs(), turn, turn.failure);
            if (result == null) return MAPPER.createObjectNode().put("success", true);
            turn.await(transport.config().timeoutMs());
            return result;
        } catch (RuntimeException failure) {
            turn.cancelDecisions();
            if (!turn.terminal.isDone()) {
                try {
                    requestInternal("autohand.abort", Map.of(), 2_000, null, null);
                    PromptTurn.awaitFuture(turn.terminal, 2_000);
                } catch (RuntimeException cleanupFailure) {
                    transport.close();
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        } finally {
            synchronized (turn) {
                turn.cancelDecisions();
                activePrompt.compareAndSet(turn, null);
            }
            promptLock.unlock();
        }
    }

    void decideStep(PromptTurn turn, String stepId, boolean stop) {
        JsonNode result = requestInternal("autohand.stepDecision", Map.of("stepId", stepId, "stop", stop),
                transport.config().timeoutMs(), turn, null);
        if (result != null && (!result.path("success").isBoolean() || !result.path("success").booleanValue())) {
            throw new TransportException("Invalid or rejected autohand.stepDecision result");
        }
    }

    public JsonNode abort(Map<String, Object> params) {
        PromptTurn turn = activePrompt.get();
        if (turn != null) turn.cancelDecisions();
        return requestInternal("autohand.abort", params == null ? Map.of() : params,
                transport.config().timeoutMs(), turn, null);
    }

    public void abortPrompt(AtomicBoolean cancellation) {
        cancellation.set(true);
        PromptTurn turn = activePrompt.get();
        if (turn != null && turn.cancellation == cancellation) {
            turn.cancelDecisions();
            requestInternal("autohand.abort", Map.of(), transport.config().timeoutMs(), turn, null);
        }
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

    public BrowserHandoff.AttachResult attachLatestBrowserHandoff() {
        return request("autohand.browserHandoff.attachLatest", Map.of(), BrowserHandoff.AttachResult.class);
    }

    public AutoMode.StartResult startAutoMode(AutoMode.StartParams params) {
        return request("autohand.automode.start", params, AutoMode.StartResult.class);
    }

    public AutoMode.StatusResult getAutoModeStatus() {
        return request("autohand.automode.status", Map.of(), AutoMode.StatusResult.class);
    }

    public AutoMode.OperationResult pauseAutoMode() {
        return request("autohand.automode.pause", Map.of(), AutoMode.OperationResult.class);
    }

    public AutoMode.OperationResult resumeAutoMode() {
        return request("autohand.automode.resume", Map.of(), AutoMode.OperationResult.class);
    }

    public AutoMode.OperationResult cancelAutoMode(AutoMode.CancelParams params) {
        return request("autohand.automode.cancel",
                params == null ? AutoMode.CancelParams.withoutReason() : params,
                AutoMode.OperationResult.class);
    }

    public AutoMode.LogResult getAutoModeLog(AutoMode.GetLogParams params) {
        return request("autohand.automode.getLog",
                params == null ? AutoMode.GetLogParams.defaults() : params,
                AutoMode.LogResult.class);
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
                    PromptTurn turn = activePrompt.get();
                    if (turn != null) {
                        turn.dispatch(toEvent(message));
                        continue;
                    }
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
            PromptTurn turn = activePrompt.get();
            if (turn != null) turn.fail(failure);
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
            case "autohand.stepEnd" -> !validStepEnd(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.StepEndEvent(params.path("stepId").textValue(),
                    MAPPER.convertValue(params.get("step"), AgentStep.class), timestamp);
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
                    "stop_condition".equals(text(params, "reason", null)) ? "stopped"
                            : text(params, "reason", "status", "completed"),
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
            case "autohand.hook.fileModified" -> !validHookFileModified(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.FileModifiedEvent(
                    text(params, "filePath", "file_path", "path", null),
                    text(params, "changeType", null),
                    text(params, "toolId", null),
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
            case "autohand.automode.iteration" -> !validAutoModeIteration(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.AutoModeIterationEvent(
                    text(params, "sessionId", null),
                    params.path("iteration").asInt(0),
                    MAPPER.convertValue(params.path("actions"), new TypeReference<List<String>>() { }),
                    params.hasNonNull("tokensUsed") ? params.get("tokensUsed").asLong() : null,
                    timestamp);
            case "autohand.automode.complete" -> !validAutoModeComplete(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.AutoModeCompleteEvent(
                    text(params, "sessionId", null),
                    params.path("iterations").asInt(0),
                    params.path("filesCreated").asInt(0),
                    params.path("filesModified").asInt(0),
                    timestamp);
            case "autohand.automode.error" -> !validAutoModeError(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.AutoModeErrorEvent(
                    text(params, "sessionId", null),
                    text(params, "error", null),
                    timestamp);
            case "autohand.hook.preTool" -> !validHookPreTool(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookPreToolEvent(
                    text(params, "toolId", null),
                    text(params, "toolName", null),
                    MAPPER.convertValue(params.path("args"), new TypeReference<Map<String, Object>>() { }),
                    timestamp);
            case "autohand.hook.postTool" -> !validHookPostTool(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookPostToolEvent(
                    text(params, "toolId", null),
                    text(params, "toolName", null),
                    params.path("success").asBoolean(false),
                    params.path("duration").asDouble(0),
                    text(params, "output", null),
                    timestamp);
            case "autohand.hook.prePrompt" -> !validHookPrePrompt(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookPrePromptEvent(
                    text(params, "instruction", null),
                    MAPPER.convertValue(params.path("mentionedFiles"), new TypeReference<List<String>>() { }),
                    timestamp);
            case "autohand.hook.postResponse" -> !validHookPostResponse(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookPostResponseEvent(
                    params.path("tokensUsed").asLong(0),
                    tokenUsageStatus(text(params, "tokensUsageStatus", null)),
                    params.path("toolCallsCount").asInt(0),
                    params.path("duration").asDouble(0),
                    timestamp);
            case "autohand.hook.sessionError" -> !validHookSessionError(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookSessionErrorEvent(
                    text(params, "error", null),
                    text(params, "code", null),
                    params.hasNonNull("context")
                            ? MAPPER.convertValue(params.path("context"), new TypeReference<Map<String, Object>>() { })
                            : null,
                    timestamp);
            case "autohand.hook.stop" -> !validHookPostResponse(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookStopEvent(
                    params.path("tokensUsed").asLong(0),
                    tokenUsageStatus(text(params, "tokensUsageStatus", null)),
                    params.path("toolCallsCount").asInt(0),
                    params.path("duration").asDouble(0),
                    timestamp);
            case "autohand.hook.sessionStart" -> !validHookSessionStart(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookSessionStartEvent(
                    hookSessionType(text(params, "sessionType", null)),
                    timestamp);
            case "autohand.hook.sessionEnd" -> !validHookSessionEnd(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookSessionEndEvent(
                    hookSessionEndReason(text(params, "reason", null)),
                    params.path("duration").asDouble(0),
                    timestamp);
            case "autohand.hook.subagentStop" -> !validHookSubagentStop(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookSubagentStopEvent(
                    text(params, "subagentId", null),
                    text(params, "subagentName", null),
                    text(params, "subagentType", null),
                    params.path("success").asBoolean(false),
                    params.path("duration").asDouble(0),
                    text(params, "error", null),
                    timestamp);
            case "autohand.hook.permissionRequest" -> !validHookPermissionRequest(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookPermissionRequestEvent(
                    text(params, "tool", null),
                    text(params, "path", null),
                    text(params, "command", null),
                    params.hasNonNull("args")
                            ? MAPPER.convertValue(params.path("args"), new TypeReference<Map<String, Object>>() { })
                            : null,
                    timestamp);
            case "autohand.hook.notification" -> !validHookNotification(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookNotificationEvent(
                    text(params, "notificationType", null),
                    text(params, "message", null),
                    timestamp);
            case "autohand.hook.contextCompacted" -> !validHookContextCompacted(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookContextCompactedEvent(
                    params.path("croppedCount").asLong(0),
                    text(params, "summary", null),
                    params.path("usagePercent").asDouble(0),
                    text(params, "reason", null),
                    timestamp);
            case "autohand.hook.contextOverflow" -> !validHookContextOverflow(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookContextOverflowEvent(
                    params.path("tokensBefore").asLong(0),
                    params.path("tokensAfter").asLong(0),
                    params.path("croppedCount").asLong(0),
                    params.path("usagePercent").asDouble(0),
                    timestamp);
            case "autohand.hook.contextWarning" -> !validHookContextUsage(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookContextWarningEvent(
                    params.path("usagePercent").asDouble(0),
                    params.path("remainingTokens").asLong(0),
                    timestamp);
            case "autohand.hook.contextCritical" -> !validHookContextUsage(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.HookContextCriticalEvent(
                    params.path("usagePercent").asDouble(0),
                    params.path("remainingTokens").asLong(0),
                    timestamp);
            case "autohand.mcp.invokeRequest" -> !validMcpInvocationRequest(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.McpInvocationRequestEvent(
                    text(params, "requestId", null),
                    text(params, "toolName", null),
                    MAPPER.convertValue(params.path("args"), new TypeReference<Map<String, Object>>() { }),
                    timestamp);
            case "autohand.mcp.toolsChanged" -> !validMcpToolsChanged(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.McpToolsChangedEvent(
                    MAPPER.convertValue(params.path("tools"), new TypeReference<List<Events.McpTool>>() { }),
                    timestamp);
            case "autohand.learn.progress" -> !validLearnProgress(params)
                    ? new Events.UnknownEvent(method, params, timestamp)
                    : new Events.LearnProgressEvent(
                    learnProgressStatus(text(params, "status", null)),
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

    private static Events.TokenUsageStatus tokenUsageStatus(String value) {
        return switch (value == null ? "" : value) {
            case "actual" -> Events.TokenUsageStatus.ACTUAL;
            case "unavailable" -> Events.TokenUsageStatus.UNAVAILABLE;
            default -> null;
        };
    }

    private static Events.FileChangeType fileChangeType(String value) {
        return switch (value == null ? "" : value) {
            case "create" -> Events.FileChangeType.CREATE;
            case "modify" -> Events.FileChangeType.MODIFY;
            case "delete" -> Events.FileChangeType.DELETE;
            default -> null;
        };
    }

    private static Events.HookSessionType hookSessionType(String value) {
        return switch (value == null ? "" : value) {
            case "startup" -> Events.HookSessionType.STARTUP;
            case "resume" -> Events.HookSessionType.RESUME;
            case "clear" -> Events.HookSessionType.CLEAR;
            default -> null;
        };
    }

    private static Events.HookSessionEndReason hookSessionEndReason(String value) {
        return switch (value == null ? "" : value) {
            case "quit" -> Events.HookSessionEndReason.QUIT;
            case "clear" -> Events.HookSessionEndReason.CLEAR;
            case "exit" -> Events.HookSessionEndReason.EXIT;
            case "error" -> Events.HookSessionEndReason.ERROR;
            default -> null;
        };
    }

    private static Events.LearnProgressStatus learnProgressStatus(String value) {
        return switch (value == null ? "" : value) {
            case "analyzing" -> Events.LearnProgressStatus.ANALYZING;
            case "loading-registry" -> Events.LearnProgressStatus.LOADING_REGISTRY;
            case "evaluating" -> Events.LearnProgressStatus.EVALUATING;
            case "generating" -> Events.LearnProgressStatus.GENERATING;
            case "updating" -> Events.LearnProgressStatus.UPDATING;
            default -> null;
        };
    }

    private static boolean validAutoModeIteration(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "sessionId")
                && integral(params, "iteration")
                && stringArray(params, "actions")
                && optionalIntegral(params, "tokensUsed");
    }

    private static boolean validAutoModeComplete(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "sessionId")
                && integral(params, "iterations")
                && integral(params, "filesCreated")
                && integral(params, "filesModified");
    }

    private static boolean validAutoModeError(JsonNode params) {
        return validTimestamp(params) && textual(params, "sessionId") && textual(params, "error");
    }

    private static boolean validHookPreTool(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "toolId")
                && textual(params, "toolName")
                && params.path("args").isObject();
    }

    private static boolean validHookPostTool(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "toolId")
                && textual(params, "toolName")
                && bool(params, "success")
                && finiteNumber(params, "duration")
                && optionalTextual(params, "output");
    }

    private static boolean validHookPrePrompt(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "instruction")
                && stringArray(params, "mentionedFiles");
    }

    private static boolean validHookPostResponse(JsonNode params) {
        JsonNode status = params.get("tokensUsageStatus");
        return validTimestamp(params)
                && longIntegral(params, "tokensUsed")
                && intIntegral(params, "toolCallsCount")
                && finiteNumber(params, "duration")
                && (status == null || status.isNull()
                        || status.isTextual() && tokenUsageStatus(status.textValue()) != null);
    }

    private static boolean validHookFileModified(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "filePath")
                && textual(params, "changeType")
                && fileChangeType(params.path("changeType").textValue()) != null
                && textual(params, "toolId");
    }

    private static boolean validHookSessionError(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "error")
                && optionalTextual(params, "code")
                && optionalObject(params, "context");
    }

    private static boolean validHookSessionStart(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "sessionType")
                && hookSessionType(params.path("sessionType").textValue()) != null;
    }

    private static boolean validHookSessionEnd(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "reason")
                && hookSessionEndReason(params.path("reason").textValue()) != null
                && finiteNumber(params, "duration");
    }

    private static boolean validHookSubagentStop(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "subagentId")
                && textual(params, "subagentName")
                && textual(params, "subagentType")
                && bool(params, "success")
                && finiteNumber(params, "duration")
                && optionalTextual(params, "error");
    }

    private static boolean validHookPermissionRequest(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "tool")
                && optionalTextual(params, "path")
                && optionalTextual(params, "command")
                && optionalObject(params, "args");
    }

    private static boolean validHookNotification(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "notificationType")
                && textual(params, "message");
    }

    private static boolean validHookContextCompacted(JsonNode params) {
        return validTimestamp(params)
                && nonNegativeIntegral(params, "croppedCount")
                && optionalTextual(params, "summary")
                && nonNegativeFiniteNumber(params, "usagePercent")
                && textual(params, "reason");
    }

    private static boolean validHookContextOverflow(JsonNode params) {
        return validTimestamp(params)
                && nonNegativeIntegral(params, "tokensBefore")
                && nonNegativeIntegral(params, "tokensAfter")
                && nonNegativeIntegral(params, "croppedCount")
                && nonNegativeFiniteNumber(params, "usagePercent");
    }

    private static boolean validHookContextUsage(JsonNode params) {
        return validTimestamp(params)
                && nonNegativeFiniteNumber(params, "usagePercent")
                && nonNegativeIntegral(params, "remainingTokens");
    }

    private static boolean validMcpInvocationRequest(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "requestId")
                && textual(params, "toolName")
                && params.path("args").isObject();
    }

    private static boolean validMcpToolsChanged(JsonNode params) {
        JsonNode tools = params.path("tools");
        if (!validTimestamp(params) || !tools.isArray()) {
            return false;
        }
        for (JsonNode tool : tools) {
            if (!textual(tool, "name") || !textual(tool, "description") || !textual(tool, "serverName")) {
                return false;
            }
        }
        return true;
    }

    private static boolean validLearnProgress(JsonNode params) {
        return validTimestamp(params)
                && textual(params, "status")
                && learnProgressStatus(params.path("status").textValue()) != null;
    }

    private static boolean validTimestamp(JsonNode params) {
        return params.isObject() && textual(params, "timestamp");
    }

    private static boolean validStepEnd(JsonNode params) {
        JsonNode step = params.path("step");
        if (!textual(params, "stepId") || !textual(params, "timestamp") || !step.isObject()
                || !intIntegral(step, "stepNumber") || step.path("stepNumber").intValue() < 1
                || !step.path("toolCalls").isArray() || !step.path("toolResults").isArray()
                || !absentOrText(step, "thought")) return false;
        for (JsonNode call : step.get("toolCalls")) {
            if (!textual(call, "tool") || !call.path("args").isObject() || !absentOrText(call, "id")) return false;
        }
        for (JsonNode result : step.get("toolResults")) {
            if (!textual(result, "tool") || !bool(result, "success") || !absentOrText(result, "output")
                    || !absentOrText(result, "error")) return false;
        }
        return true;
    }

    private static boolean absentOrText(JsonNode params, String field) {
        return !params.has(field) || textual(params, field);
    }

    private static boolean textual(JsonNode params, String field) {
        return params.path(field).isTextual();
    }

    private static boolean integral(JsonNode params, String field) {
        return params.path(field).isIntegralNumber();
    }

    private static boolean longIntegral(JsonNode params, String field) {
        return integral(params, field) && params.path(field).canConvertToLong();
    }

    private static boolean intIntegral(JsonNode params, String field) {
        return integral(params, field) && params.path(field).canConvertToInt();
    }

    private static boolean nonNegativeIntegral(JsonNode params, String field) {
        return longIntegral(params, field)
                && params.path(field).asLong() >= 0;
    }

    private static boolean number(JsonNode params, String field) {
        return params.path(field).isNumber();
    }

    private static boolean finiteNumber(JsonNode params, String field) {
        return number(params, field) && Double.isFinite(params.path(field).asDouble());
    }

    private static boolean nonNegativeFiniteNumber(JsonNode params, String field) {
        return finiteNumber(params, field) && params.path(field).asDouble() >= 0;
    }

    private static boolean bool(JsonNode params, String field) {
        return params.path(field).isBoolean();
    }

    private static boolean optionalIntegral(JsonNode params, String field) {
        JsonNode value = params.get(field);
        return value == null || value.isNull() || value.isIntegralNumber();
    }

    private static boolean optionalTextual(JsonNode params, String field) {
        JsonNode value = params.get(field);
        return value == null || value.isNull() || value.isTextual();
    }

    private static boolean optionalObject(JsonNode params, String field) {
        JsonNode value = params.get(field);
        return value == null || value.isNull() || value.isObject();
    }

    private static boolean stringArray(JsonNode params, String field) {
        JsonNode values = params.path(field);
        if (!values.isArray()) {
            return false;
        }
        for (JsonNode value : values) {
            if (!value.isTextual()) {
                return false;
            }
        }
        return true;
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
