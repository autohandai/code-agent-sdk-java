import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 06 Prompt Skills - Skills mentioned in prompt, SDK has them available.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="PromptSkills"
 */
public class PromptSkills {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"), cliPath, false, 300_000,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        ));

        // Add skills after construction
        sdk.setSkills(java.util.List.of(
            new SkillReference("typescript", null, null),
            new SkillReference("testing", null, null),
            new SkillReference("react", null, null),
            new SkillReference("nodejs", null, null)
        ));

        try {
            sdk.start();
            System.out.println("SDK started\n");

            String prompt = "Review this TypeScript code using /skill typescript best practices and suggest improvements.";
            System.out.println("Sending prompt: \"" + prompt + "\"\n");
            System.out.println("The agent can reference pre-loaded skills via /skill syntax\n");

            sdk.streamPrompt(new PromptParams(prompt), PromptSkills::handleEvent);

            sdk.stop();
            System.out.println("\nSDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }

    static void handleEvent(Event event) {
        switch (event) {
            case Events.AgentStartEvent e -> {
                System.out.println("[Agent started: " + e.sessionId() + "]");
                System.out.println("  Model: " + e.model());
            }
            case Events.ToolStartEvent e -> System.out.println("\n[Tool called: " + e.toolName() + "]");
            case Events.ToolUpdateEvent e -> System.out.print(e.output());
            case Events.ToolEndEvent e -> System.out.println("[Tool completed: " + e.toolName() + "]");
            case Events.PermissionRequestEvent e -> {
                System.out.println("\n[Permission request: " + e.tool() + "]");
                System.out.println("  Description: " + e.description());
            }
            case Events.MessageUpdateEvent e -> {
                if (e.delta() != null) System.out.print(e.delta());
            }
            case Events.MessageEndEvent e -> {
                if (e.content() != null) System.out.println("\n[Message completed]");
            }
            case Events.AgentEndEvent e -> System.out.println("\n[Agent ended]");
            case Events.ErrorEvent e -> System.err.println("\n[Error: " + e.message() + "]");
            default -> {}
        }
    }
}
