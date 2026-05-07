import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 21 SDLC workflow: plan first, execute only after an explicit gate.
 *
 * By default this example stops after planning. Set AUTOHAND_EXECUTE_PLAN=1
 * to disable plan mode and ask the agent to implement the approved plan.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand AUTOHAND_EXECUTE_PLAN=1 mvn compile exec:java -Dexec.mainClass="SdlcGatedImplementation"
 */
public class SdlcGatedImplementation {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String model = System.getenv("AUTOHAND_MODEL");
        boolean executePlan = "1".equals(System.getenv("AUTOHAND_EXECUTE_PLAN"));

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

            String planPrompt = String.join("\n", java.util.List.of(
                "Create an implementation plan for the requested SDK change.",
                "Use repository inspection only.",
                "Do not edit files in this planning pass.",
                "Return numbered steps with test coverage and rollback notes."
            ));

            System.out.println("--- planning ---\n");
            streamPrompt(sdk, planPrompt);

            if (!executePlan) {
                System.out.println("\n--- gate closed ---");
                System.out.println("Set AUTOHAND_EXECUTE_PLAN=1 after reviewing the plan to run the implementation phase.");
                return;
            }

            sdk.disablePlanMode();

            String executePrompt = String.join("\n", java.util.List.of(
                "Implement the approved plan.",
                "Keep changes scoped.",
                "Run the relevant checks with Maven.",
                "Summarize changed files and verification results."
            ));

            System.out.println("\n--- implementation ---\n");
            streamPrompt(sdk, executePrompt);
        } finally {
            sdk.stop();
        }
    }

    static void streamPrompt(AutohandSDK sdk, String message) throws Exception {
        sdk.streamPrompt(new PromptParams(message), event -> {
            switch (event) {
                case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                case Events.ToolStartEvent e -> System.out.println("\n[tool:start] " + e.toolName());
                case Events.ToolUpdateEvent e -> System.out.print(e.output());
                case Events.ToolEndEvent e -> System.out.println("[tool:end] " + e.toolName() + " success=" + e.success());
                case Events.PermissionRequestEvent e -> System.out.println("\n[permission] " + e.tool() + ": " + e.description());
                case Events.ErrorEvent e -> System.err.println("\n[error] " + e.message());
                default -> {}
            }
        });
    }
}
