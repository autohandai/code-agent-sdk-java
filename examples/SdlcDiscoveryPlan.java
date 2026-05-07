import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 20 SDLC workflow: discovery and planning.
 *
 * Runs the agent in plan mode so it can inspect the project and
 * produce an implementation plan without performing write operations.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="SdlcDiscoveryPlan"
 */
public class SdlcDiscoveryPlan {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String model = System.getenv("AUTOHAND_MODEL");

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"), cliPath, false, 300_000,
            model, null, null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        ));
        sdk.setSkills(java.util.List.of(
            new SkillReference("typescript", null, null),
            new SkillReference("testing", null, null)
        ));

        try {
            sdk.start();
            sdk.enablePlanMode();

            String prompt = String.join("\n", java.util.List.of(
                "We are in discovery for a production TypeScript SDK change.",
                "Inspect the repository and produce an SDLC plan only.",
                "Do not edit files.",
                "Include scope, risks, test strategy, rollout steps, and explicit non-goals."
            ));
            streamPrompt(sdk, prompt);
        } finally {
            sdk.stop();
        }
    }

    static void streamPrompt(AutohandSDK sdk, String message) throws Exception {
        sdk.streamPrompt(new PromptParams(message), event -> {
            switch (event) {
                case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                case Events.ToolStartEvent e -> System.out.println("\n[tool:start] " + e.toolName());
                case Events.ToolEndEvent e -> System.out.println("[tool:end] " + e.toolName() + " success=" + e.success());
                case Events.PermissionRequestEvent e -> System.out.println("\n[permission] " + e.tool() + ": " + e.description());
                case Events.ErrorEvent e -> System.err.println("\n[error] " + e.message());
                default -> {}
            }
        });
    }
}
