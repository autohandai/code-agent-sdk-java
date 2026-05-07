package ai.autohand.sdk.sdk;

import ai.autohand.sdk.rpc.RPCClient;
import ai.autohand.sdk.transport.Transport;
import ai.autohand.sdk.transport.TransportConfig;
import ai.autohand.sdk.types.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class AutohandSDK implements AutoCloseable {
    private final SDKConfig config;
    private final Transport transport;
    private final RPCClient rpc;

    public AutohandSDK() {
        this(new SDKConfig(".", null, false, 300_000));
    }

    public AutohandSDK(SDKConfig config) {
        this.config = config;
        this.transport = new Transport(new TransportConfig(config.cwd(), config.cliPath(), config.debug(), config.timeoutMs()));
        this.rpc = new RPCClient(transport);
    }

    public void start() throws Exception {
        transport.start();
    }

    public void stop() {
        transport.close();
    }

    @Override
    public void close() {
        stop();
    }

    public boolean isRunning() {
        return transport.isRunning();
    }

    public void prompt(PromptParams params) {
        rpc.prompt(params, event -> {
        });
    }

    public void streamPrompt(PromptParams params, Consumer<Event> onEvent) {
        rpc.prompt(params, onEvent);
    }

    public void setPermissionMode(PermissionMode mode) {
    }

    public void setPlanMode(boolean enabled) {
    }

    public void enablePlanMode() {
        setPlanMode(true);
    }

    public void disablePlanMode() {
        setPlanMode(false);
    }

    public void setModel(String model) {
    }

    public void setMaxThinkingTokens(int tokens) {
    }

    public void applyFlagSettings(Map<String, Object> settings) {
    }

    public List<String> supportedModels() {
        return List.of();
    }

    public Map<String, Object> getContextUsage() {
        return Map.of();
    }

    public void reloadPlugins() {
    }

    public Map<String, Object> accountInfo() {
        return Map.of();
    }

    public void toggleMCPServer(String serverName, boolean enabled) {
    }

    public void reconnectMCPServer(String serverName) {
    }

    public void setMCPServers(Map<String, McpServerConfig> servers) {
    }

    public SessionMetadata getSessionMetadata() {
        return new SessionMetadata("session", "project", config.model());
    }

    public SessionStats getStats() {
        return new SessionStats(0, 0, 0, 0);
    }

    public void saveSession() {
    }

    public void resumeSession(String sessionId) {
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
    }

    public void permissionResponse(String requestId, PermissionDecision decision) {
        permissionResponse(new PermissionResponseParams(requestId, decision, null, null, null));
    }

    public static String createDefaultAgentsMd(String projectName) {
        String name = projectName == null || projectName.isBlank() ? "Project" : projectName;
        return "# " + name + " Agent Guidance\n\nDescribe architecture, commands, and conventions here.\n";
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
