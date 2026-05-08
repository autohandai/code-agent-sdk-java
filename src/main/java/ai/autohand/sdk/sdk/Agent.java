package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.DecisionScope;
import ai.autohand.sdk.types.PermissionDecision;
import ai.autohand.sdk.types.PermissionMode;
import ai.autohand.sdk.types.SDKConfig;

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
        this.sdk.start();

        if (options.permissionMode() != null) {
            this.sdk.setPermissionMode(options.permissionMode());
        }
        if (options.planMode()) {
            this.sdk.enablePlanMode();
        }
    }

    public static Agent create(AgentOptions options) throws IOException {
        return new Agent(options);
    }

    public Run send(String prompt) {
        return new Run(sdk, prompt);
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
