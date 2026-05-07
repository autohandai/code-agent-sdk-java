import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 07 Direct Skills - Skills provided directly via SDK with file paths.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="DirectSkills"
 */
public class DirectSkills {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        List<SkillReference> skills = new ArrayList<>();
        skills.add(new SkillReference("typescript", null, null));
        skills.add(new SkillReference("testing", null, null));
        // File paths are auto-detected by containing '/' or '.md'
        // skills.add(new SkillReference("./skills/my-custom/SKILL.md", null, null));

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
        sdk.setSkills(skills);

        try {
            sdk.start();
            System.out.println("SDK started");
            System.out.println("Skills loaded: " + skills.stream().map(SkillReference::name).toList());
            System.out.println();

            String prompt = "Review this codebase and suggest improvements.";
            System.out.println("Sending prompt: \"" + prompt + "\"\n");
            System.out.println("(Skills are pre-loaded and available to the agent)\n");

            sdk.streamPrompt(new PromptParams(prompt), DirectSkills::handleEvent);

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
