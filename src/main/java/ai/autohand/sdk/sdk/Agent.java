package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.DecisionScope;
import ai.autohand.sdk.types.PermissionDecision;
import java.util.Map;

public final class Agent implements AutoCloseable {
    private final AgentOptions options;
    private final AutohandSDK sdk;

    private Agent(AgentOptions options) throws Exception {
        this.options = options;
        this.sdk = new AutohandSDK(new ai.autohand.sdk.types.SDKConfig(
                options.cwd(),
                options.cliPath(),
                false,
                300_000,
                options.model()
        ));
        this.sdk.start();
    }

    public static Agent create(AgentOptions options) throws Exception {
        return new Agent(options);
    }

    public Run send(String prompt) {
        return new Run(prompt);
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
        return runJson(prompt, type);
    }

    public void allowPermission(String requestId, DecisionScope scope) throws Exception {
        sdk.allowPermission(requestId, scope);
    }

    public void denyPermission(String requestId, DecisionScope scope) throws Exception {
        sdk.denyPermission(requestId, scope);
    }

    public void permissionResponse(String requestId, PermissionDecision decision) throws Exception {
        sdk.permissionResponse(requestId, decision);
    }

    public AgentOptions options() {
        return options;
    }

    @Override
    public void close() {
        sdk.stop();
    }
}
