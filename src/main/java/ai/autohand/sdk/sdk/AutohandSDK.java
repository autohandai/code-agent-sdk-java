package ai.autohand.sdk.sdk;

import ai.autohand.sdk.rpc.RPCClient;
import ai.autohand.sdk.transport.Transport;
import ai.autohand.sdk.transport.TransportConfig;
import ai.autohand.sdk.types.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class AutohandSDK implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final SDKConfig baseConfig;
    private Transport transport;
    private RPCClient client;
    private String modelOverride;
    private String systemPromptOverride;
    private String appendSystemPromptOverride;
    private String sessionIdOverride;
    private List<SkillReference> skills;
    private PermissionMode pendingPermissionMode;
    private Boolean pendingPlanMode;

    public AutohandSDK() {
        this(new SDKConfig(".", null, false, 300_000));
    }

    public AutohandSDK(SDKConfig config) {
        this.baseConfig = config;
        this.modelOverride = config.model();
        this.systemPromptOverride = config.systemPrompt();
        this.appendSystemPromptOverride = config.appendSystemPrompt();
        this.sessionIdOverride = null;
        this.skills = new ArrayList<>(config.skills());
        rebuildClient();
    }

    public synchronized void start() throws IOException {
        if (isRunning()) {
            return;
        }
        rebuildClient();
        try {
            transport.start();
            if (baseConfig.features() != null) {
                Map<String, Object> features = MAPPER.convertValue(baseConfig.features(),
                        MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
                client.applyFlagSettings(Map.of("features", features));
            }
            if (pendingPermissionMode != null) {
                client.setPermissionMode(pendingPermissionMode);
            }
            if (pendingPlanMode != null) {
                client.setPlanMode(pendingPlanMode);
            }
        } catch (IOException | RuntimeException exception) {
            transport.close();
            throw exception;
        }
    }

    public void stop() {
        if (transport != null) {
            transport.close();
        }
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return transport != null && transport.isRunning();
    }

    public PromptResult prompt(PromptParams params) {
        ensureStarted();
        JsonNode result = client.request("autohand.prompt", params);
        return result.isMissingNode() || result.isEmpty() ? new PromptResult(true) : RPCClient.convert(result, PromptResult.class);
    }

    public void streamPrompt(PromptParams params, Consumer<Event> onEvent) {
        ensureStarted();
        client.prompt(params, onEvent);
    }

    public void setPermissionMode(PermissionMode mode) {
        this.pendingPermissionMode = mode;
        if (isRunning()) {
            client.setPermissionMode(mode);
        }
    }

    public void setPlanMode(boolean enabled) {
        this.pendingPlanMode = enabled;
        if (isRunning()) {
            client.setPlanMode(enabled);
        }
    }

    public void enablePlanMode() {
        setPlanMode(true);
    }

    public void disablePlanMode() {
        setPlanMode(false);
    }

    public void setModel(String model) {
        this.modelOverride = model;
        if (isRunning()) {
            client.setModel(model);
        }
    }

    public void setSystemPrompt(String systemPrompt) {
        ensureNotRunning("setSystemPrompt");
        this.systemPromptOverride = systemPrompt;
    }

    public void appendSystemPrompt(String appendSystemPrompt) {
        ensureNotRunning("appendSystemPrompt");
        this.appendSystemPromptOverride = appendSystemPrompt;
    }

    public void setSkills(List<SkillReference> skills) {
        ensureNotRunning("setSkills");
        this.skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public void setMaxThinkingTokens(int tokens) {
        ensureStarted();
        client.setMaxThinkingTokens(tokens);
    }

    public void clearMaxThinkingTokens() {
        ensureStarted();
        client.setMaxThinkingTokens(null);
    }

    public void applyFlagSettings(Map<String, Object> settings) {
        ensureStarted();
        client.applyFlagSettings(settings);
    }

    public List<ModelInfo> supportedModels() {
        ensureStarted();
        JsonNode result = client.getSupportedModels();
        JsonNode models = result.has("models") ? result.get("models") : result;
        if (models == null || !models.isArray()) {
            return List.of();
        }
        return MAPPER.convertValue(models, MAPPER.getTypeFactory().constructCollectionType(List.class, ModelInfo.class));
    }

    public List<String> supportedCommands() {
        ensureStarted();
        JsonNode result = client.getSupportedCommands();
        JsonNode commands = result.has("commands") ? result.get("commands") : result;
        if (commands == null || !commands.isArray()) {
            return List.of();
        }
        List<String> values = MAPPER.convertValue(commands,
                MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
        return values.stream().map(command -> command.startsWith("/") ? command : "/" + command).toList();
    }

    public boolean supportsCommand(String command) {
        String normalized = command.startsWith("/") ? command : "/" + command;
        return supportedCommands().contains(normalized);
    }

    public Goals.SnapshotResult getGoal() {
        ensureStarted();
        return client.getGoal();
    }

    public Goals.MutationResult createGoal(Goals.CreateParams params) {
        ensureStarted();
        return client.createGoal(params);
    }

    public Goals.MutationResult updateGoal(Goals.UpdateParams params) {
        ensureStarted();
        return client.updateGoal(params);
    }

    public Goals.MutationResult clearGoal() {
        ensureStarted();
        return client.clearGoal();
    }

    public Goals.MutationResult queueGoal(Goals.CreateParams params) {
        ensureStarted();
        return client.queueGoal(params);
    }

    public Goals.MutationResult startQueuedGoal() {
        ensureStarted();
        return client.startQueuedGoal();
    }

    public Goals.TemplatesResult listGoalTemplates() {
        ensureStarted();
        return client.listGoalTemplates();
    }

    public Autoresearch.StartResult startAutoresearch(Autoresearch.StartParams params) {
        ensureStarted();
        return client.startAutoresearch(params);
    }

    public Autoresearch.StatusResult getAutoresearchStatus() {
        ensureStarted();
        return client.getAutoresearchStatus();
    }

    public Autoresearch.StopResult stopAutoresearch() {
        ensureStarted();
        return client.stopAutoresearch();
    }

    public Autoresearch.HistoryResult getAutoresearchHistory() {
        ensureStarted();
        return client.getAutoresearchHistory();
    }

    public Autoresearch.ReplayResult replayAutoresearch(Autoresearch.ReplayParams params) {
        ensureStarted();
        return client.replayAutoresearch(params);
    }

    public Autoresearch.RescoreResult rescoreAutoresearch(Autoresearch.RescoreParams params) {
        ensureStarted();
        return client.rescoreAutoresearch(params);
    }

    public Autoresearch.CompareResult compareAutoresearch(Autoresearch.CompareParams params) {
        ensureStarted();
        return client.compareAutoresearch(params);
    }

    public Autoresearch.ParetoResult getAutoresearchPareto() {
        ensureStarted();
        return client.getAutoresearchPareto();
    }

    public Autoresearch.PinResult pinAutoresearch(Autoresearch.PinParams params) {
        ensureStarted();
        return client.pinAutoresearch(params);
    }

    public Autoresearch.PruneResult pruneAutoresearch(Autoresearch.PruneParams params) {
        ensureStarted();
        return client.pruneAutoresearch(params);
    }

    public GetStateResult getState() {
        ensureStarted();
        return RPCClient.convert(client.getState(), GetStateResult.class);
    }

    public GetMessagesResult getMessages() {
        ensureStarted();
        return RPCClient.convert(client.getMessages(), GetMessagesResult.class);
    }

    public Conversation.ResetResult reset() {
        ensureStarted();
        return client.reset();
    }

    public BrowserHandoff.CreateResult createBrowserHandoff() {
        return createBrowserHandoff(BrowserHandoff.CreateParams.defaults());
    }

    public BrowserHandoff.CreateResult createBrowserHandoff(BrowserHandoff.CreateParams params) {
        ensureStarted();
        return client.createBrowserHandoff(params);
    }

    public BrowserHandoff.AttachResult attachBrowserHandoff(BrowserHandoff.AttachParams params) {
        ensureStarted();
        return client.attachBrowserHandoff(params);
    }

    public BrowserHandoff.AttachResult attachLatestBrowserHandoff() {
        ensureStarted();
        return client.attachLatestBrowserHandoff();
    }

    public AutoMode.StartResult startAutoMode(AutoMode.StartParams params) {
        ensureStarted();
        return client.startAutoMode(params);
    }

    public AutoMode.StatusResult getAutoModeStatus() {
        ensureStarted();
        return client.getAutoModeStatus();
    }

    public AutoMode.OperationResult pauseAutoMode() {
        ensureStarted();
        return client.pauseAutoMode();
    }

    public AutoMode.OperationResult resumeAutoMode() {
        ensureStarted();
        return client.resumeAutoMode();
    }

    public AutoMode.OperationResult cancelAutoMode() {
        return cancelAutoMode(AutoMode.CancelParams.withoutReason());
    }

    public AutoMode.OperationResult cancelAutoMode(AutoMode.CancelParams params) {
        ensureStarted();
        return client.cancelAutoMode(params);
    }

    public AutoMode.LogResult getAutoModeLog() {
        return getAutoModeLog(AutoMode.GetLogParams.defaults());
    }

    public AutoMode.LogResult getAutoModeLog(AutoMode.GetLogParams params) {
        ensureStarted();
        return client.getAutoModeLog(params);
    }

    public CommunitySkills.RegistryResult getSkillsRegistry() {
        return getSkillsRegistry(CommunitySkills.RegistryParams.cached());
    }

    public CommunitySkills.RegistryResult getSkillsRegistry(CommunitySkills.RegistryParams params) {
        ensureStarted();
        return client.getSkillsRegistry(params);
    }

    public CommunitySkills.InstallResult installSkill(CommunitySkills.InstallParams params) {
        ensureStarted();
        return client.installSkill(params);
    }

    public ContextUsage getContextUsage() {
        ensureStarted();
        return RPCClient.convert(client.getContextUsage(), ContextUsage.class);
    }

    public void reloadPlugins() {
        ensureStarted();
        client.reloadPlugins();
    }

    public AccountInfo accountInfo() {
        ensureStarted();
        return RPCClient.convert(client.getAccountInfo(), AccountInfo.class);
    }

    public AccountInfo getAccountInfo() {
        return accountInfo();
    }

    public void toggleMcpServer(String serverName, boolean enabled) {
        ensureStarted();
        client.toggleMcpServer(serverName, enabled);
    }

    public void reconnectMcpServer(String serverName) {
        ensureStarted();
        client.reconnectMcpServer(serverName);
    }

    public void setMcpServers(Map<String, McpServerConfig> servers) {
        ensureStarted();
        client.setMcpServers(servers);
    }

    public McpDiscovery.ListServersResult listMcpServers() {
        ensureStarted();
        return client.listMcpServers();
    }

    public McpDiscovery.ListToolsResult listMcpTools() {
        return listMcpTools(McpDiscovery.ListToolsParams.allServers());
    }

    public McpDiscovery.ListToolsResult listMcpTools(McpDiscovery.ListToolsParams params) {
        ensureStarted();
        return client.listMcpTools(params);
    }

    public McpDiscovery.GetServerConfigsResult getMcpServerConfigs() {
        ensureStarted();
        return client.getMcpServerConfigs();
    }

    public void toggleMCPServer(String serverName, boolean enabled) {
        toggleMcpServer(serverName, enabled);
    }

    public void reconnectMCPServer(String serverName) {
        reconnectMcpServer(serverName);
    }

    public void setMCPServers(Map<String, McpServerConfig> servers) {
        setMcpServers(servers);
    }

    public SessionMetadata getSessionMetadata() {
        GetStateResult state = getState();
        return new SessionMetadata(
                state.sessionId(),
                baseConfig.cwd(),
                state.model() == null ? modelOverride : state.model());
    }

    public SessionStats getStats() {
        GetMessagesResult messages = getMessages();
        return new SessionStats(0, messages.messages() == null ? 0 : messages.messages().size(), 0, 0);
    }

    public void saveSession() {
        ensureStarted();
        client.saveSession();
    }

    public void resumeSession(String sessionId) {
        if (isRunning()) {
            client.resumeSession(sessionId);
        } else {
            this.sessionIdOverride = sessionId;
        }
    }

    public void allowPermission(String requestId, DecisionScope scope) {
        permissionResponse(requestId, allowDecision(scope));
    }

    public void denyPermission(String requestId, DecisionScope scope) {
        permissionResponse(requestId, denyDecision(scope));
    }

    public void suggestPermissionAlternative(String requestId, String alternative) {
        permissionResponse(new PermissionResponseParams(requestId, PermissionDecision.ALTERNATIVE, false, alternative, null));
    }

    public void permissionResponse(PermissionResponseParams params) {
        ensureStarted();
        client.permissionResponse(params);
    }

    public void permissionResponse(String requestId, PermissionDecision decision) {
        permissionResponse(new PermissionResponseParams(requestId, decision, null, null, null));
    }

    public HookResultTypes.GetHooksResult getHooks() {
        ensureStarted();
        JsonNode result = client.getHooks();
        if (result.has("settings")) {
            return RPCClient.convert(result, HookResultTypes.GetHooksResult.class);
        }
        HooksSettings settings = MAPPER.convertValue(result, HooksSettings.class);
        return new HookResultTypes.GetHooksResult(settings);
    }

    public HookResultTypes.AddHookResult addHook(HookDefinition hook) {
        ensureStarted();
        return RPCClient.convert(client.addHook(hook), HookResultTypes.AddHookResult.class);
    }

    public HookResultTypes.RemoveHookResult removeHook(HookEvent event, int index) {
        ensureStarted();
        return RPCClient.convert(client.removeHook(event, index), HookResultTypes.RemoveHookResult.class);
    }

    public HookResultTypes.ToggleHookResult toggleHook(HookEvent event, int index) {
        ensureStarted();
        return RPCClient.convert(client.toggleHook(event, index), HookResultTypes.ToggleHookResult.class);
    }

    public RPCClient client() {
        return client;
    }

    public static String createDefaultAgentsMd(String projectName) {
        String name = projectName == null || projectName.isBlank() ? "Project" : projectName;
        return "# " + name + " Agent Guidance\n\nDescribe architecture, commands, and conventions here.\n";
    }

    private void ensureStarted() {
        if (!isRunning()) {
            throw new TransportException("AutohandSDK is not started. Call start() first, or use Agent.create(...) for the high-level API.");
        }
    }

    private void ensureNotRunning(String operation) {
        if (isRunning()) {
            throw new IllegalStateException(operation + " must be configured before start().");
        }
    }

    private void rebuildClient() {
        SDKConfig.Builder builder = baseConfig.toBuilder()
                .model(modelOverride)
                .systemPrompt(systemPromptOverride)
                .appendSystemPrompt(appendSystemPromptOverride)
                .skills(skills);
        if (sessionIdOverride != null) {
            builder.sessionId(sessionIdOverride);
        }
        SDKConfig effective = builder.build();
        this.transport = new Transport(new TransportConfig(
                effective.cwd(),
                effective.cliPath(),
                effective.debug(),
                effective.timeoutMs(),
                effective.cliArgs(),
                effective.environment()));
        this.client = new RPCClient(transport);
    }

    private static PermissionDecision allowDecision(DecisionScope scope) {
        return switch (scope) {
            case SESSION -> PermissionDecision.ALLOW_SESSION;
            case PROJECT -> PermissionDecision.ALLOW_ALWAYS_PROJECT;
            case USER -> PermissionDecision.ALLOW_ALWAYS_USER;
            case ONCE -> PermissionDecision.ALLOW_ONCE;
        };
    }

    private static PermissionDecision denyDecision(DecisionScope scope) {
        return switch (scope) {
            case SESSION -> PermissionDecision.DENY_SESSION;
            case PROJECT -> PermissionDecision.DENY_ALWAYS_PROJECT;
            case USER -> PermissionDecision.DENY_ALWAYS_USER;
            case ONCE -> PermissionDecision.DENY_ONCE;
        };
    }
}
