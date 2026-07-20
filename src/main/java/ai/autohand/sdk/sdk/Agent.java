package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.DecisionScope;
import ai.autohand.sdk.types.PermissionDecision;
import ai.autohand.sdk.types.PermissionMode;
import ai.autohand.sdk.types.SDKConfig;
import ai.autohand.sdk.types.Autoresearch;
import ai.autohand.sdk.types.BrowserHandoff;
import ai.autohand.sdk.types.CommunitySkills;
import ai.autohand.sdk.types.Conversation;
import ai.autohand.sdk.types.Goals;
import ai.autohand.sdk.types.McpDiscovery;

import java.io.IOException;
import java.util.Map;

public final class Agent implements AutoCloseable {
    private final AgentOptions options;
    private final AutohandSDK sdk;

    private Agent(AgentOptions options) throws IOException {
        this.options = options;
        SDKConfig.Builder config = SDKConfig.builder()
                .cwd(options.cwd())
                .cliPath(options.cliPath())
                .model(options.model())
                .appendSystemPrompt(options.instructions())
                .systemPrompt(options.systemPrompt())
                .skills(options.skills());

        if (options.permissionMode() == PermissionMode.UNRESTRICTED) {
            config.unrestricted(true);
        }

        this.sdk = new AutohandSDK(config.build());
        if (options.permissionMode() != null) {
            this.sdk.setPermissionMode(options.permissionMode());
        }
        if (options.planMode()) {
            this.sdk.enablePlanMode();
        }
        this.sdk.start();
    }

    public static Agent create(AgentOptions options) throws IOException {
        return new Agent(options);
    }

    public Run send(String prompt) {
        return new Run(sdk, prompt);
    }

    public Run command(String command, String arguments) {
        String normalized = command.startsWith("/") ? command : "/" + command;
        String suffix = arguments == null || arguments.isBlank() ? "" : " " + arguments.trim();
        return send(normalized + suffix);
    }

    public Run deepResearch(String topic) {
        return command("/deep-research", topic);
    }

    public Run autoresearch(String objective) {
        return command("/autoresearch", objective);
    }

    public boolean supportsCommand(String command) {
        return sdk.supportsCommand(command);
    }

    public Goals.SnapshotResult getGoal() {
        return sdk.getGoal();
    }

    public Goals.MutationResult createGoal(Goals.CreateParams params) {
        return sdk.createGoal(params);
    }

    public Goals.MutationResult updateGoal(Goals.UpdateParams params) {
        return sdk.updateGoal(params);
    }

    public Goals.MutationResult clearGoal() {
        return sdk.clearGoal();
    }

    public Goals.MutationResult queueGoal(Goals.CreateParams params) {
        return sdk.queueGoal(params);
    }

    public Goals.MutationResult startQueuedGoal() {
        return sdk.startQueuedGoal();
    }

    public Goals.TemplatesResult listGoalTemplates() {
        return sdk.listGoalTemplates();
    }

    public Autoresearch.StartResult startAutoresearch(Autoresearch.StartParams params) {
        return sdk.startAutoresearch(params);
    }

    public Autoresearch.StatusResult getAutoresearchStatus() {
        return sdk.getAutoresearchStatus();
    }

    public Autoresearch.StopResult stopAutoresearch() {
        return sdk.stopAutoresearch();
    }

    public Autoresearch.HistoryResult getAutoresearchHistory() {
        return sdk.getAutoresearchHistory();
    }

    public Autoresearch.ReplayResult replayAutoresearch(Autoresearch.ReplayParams params) {
        return sdk.replayAutoresearch(params);
    }

    public Autoresearch.RescoreResult rescoreAutoresearch(Autoresearch.RescoreParams params) {
        return sdk.rescoreAutoresearch(params);
    }

    public Autoresearch.CompareResult compareAutoresearch(Autoresearch.CompareParams params) {
        return sdk.compareAutoresearch(params);
    }

    public Autoresearch.ParetoResult getAutoresearchPareto() {
        return sdk.getAutoresearchPareto();
    }

    public Autoresearch.PinResult pinAutoresearch(Autoresearch.PinParams params) {
        return sdk.pinAutoresearch(params);
    }

    public Autoresearch.PruneResult pruneAutoresearch(Autoresearch.PruneParams params) {
        return sdk.pruneAutoresearch(params);
    }

    public CommunitySkills.RegistryResult getSkillsRegistry() {
        return sdk.getSkillsRegistry();
    }

    public Conversation.ResetResult reset() {
        return sdk.reset();
    }

    public BrowserHandoff.CreateResult createBrowserHandoff() {
        return sdk.createBrowserHandoff();
    }

    public BrowserHandoff.CreateResult createBrowserHandoff(BrowserHandoff.CreateParams params) {
        return sdk.createBrowserHandoff(params);
    }

    public BrowserHandoff.AttachResult attachBrowserHandoff(BrowserHandoff.AttachParams params) {
        return sdk.attachBrowserHandoff(params);
    }

    public BrowserHandoff.AttachResult attachLatestBrowserHandoff() {
        return sdk.attachLatestBrowserHandoff();
    }

    public CommunitySkills.RegistryResult getSkillsRegistry(CommunitySkills.RegistryParams params) {
        return sdk.getSkillsRegistry(params);
    }

    public CommunitySkills.InstallResult installSkill(CommunitySkills.InstallParams params) {
        return sdk.installSkill(params);
    }

    public McpDiscovery.ListServersResult listMcpServers() {
        return sdk.listMcpServers();
    }

    public McpDiscovery.ListToolsResult listMcpTools() {
        return sdk.listMcpTools();
    }

    public McpDiscovery.ListToolsResult listMcpTools(McpDiscovery.ListToolsParams params) {
        return sdk.listMcpTools(params);
    }

    public McpDiscovery.GetServerConfigsResult getMcpServerConfigs() {
        return sdk.getMcpServerConfigs();
    }

    public RunResult run(String prompt) {
        Run run = send(prompt);
        return run.waitForResult();
    }

    public <T> T runJson(String prompt, Class<T> type) throws StructuredOutputError {
        return send(prompt).json(type);
    }

    public <T> T runJson(String prompt, Class<T> type, String schemaName, Object schema, Map<String, Object> options)
            throws StructuredOutputError {
        String outputInstructions = options == null ? null : (String) options.getOrDefault("instructions", null);
        String jsonPrompt = prompt + "\n\n" + JsonParser.buildJsonInstruction(schemaName, schema, outputInstructions);
        return send(jsonPrompt).json(type);
    }

    public void allowPermission(String requestId, DecisionScope scope) {
        sdk.allowPermission(requestId, scope);
    }

    public void denyPermission(String requestId, DecisionScope scope) {
        sdk.denyPermission(requestId, scope);
    }

    public void permissionResponse(String requestId, PermissionDecision decision) {
        sdk.permissionResponse(requestId, decision);
    }

    public AutohandSDK sdk() {
        return sdk;
    }

    public AgentOptions options() {
        return options;
    }

    @Override
    public void close() {
        sdk.stop();
    }
}
