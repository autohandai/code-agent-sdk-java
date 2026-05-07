import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Loop Strategies - Different execution modes for the agent.
 *
 * Note: Loop strategies are configured on the CLI side. The tin-wrapper SDK
 * passes configuration to the CLI which handles the execution strategy.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="LoopStrategies"
 */
public class LoopStrategies {
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

        try {
            System.out.println("=== Loop Strategies Demo ===\n");
            System.out.println("Note: Loop strategies are configured on the CLI side.");
            System.out.println("The tin-wrapper SDK passes configuration to the CLI.\n");

            sdk.start();
            System.out.println("SDK started\n");

            String prompt = "List all Java files in the current directory and read each one. Summarize the codebase.";
            System.out.println("Sending prompt: \"" + prompt + "\"\n");

            StringBuilder fullResponse = new StringBuilder();
            sdk.streamPrompt(new PromptParams(prompt), event -> {
                switch (event) {
                    case Events.ToolStartEvent e -> System.out.println("[Tool called: " + e.toolName() + "]");
                    case Events.ToolEndEvent e -> System.out.println("[Tool completed: " + e.toolName() + "]");
                    case Events.PermissionRequestEvent e -> {
                        System.out.println("[Permission request: " + e.tool() + "]");
                        System.out.println("  Description: " + e.description());
                        try {
                            sdk.permissionResponse(new PermissionResponseParams(e.requestId(), PermissionDecision.ALLOW_ONCE, true, null, null));
                        } catch (Exception ex) {
                            System.err.println("Failed to approve: " + ex.getMessage());
                        }
                    }
                    case Events.MessageUpdateEvent e -> {
                        System.out.print(e.delta());
                        fullResponse.append(e.delta());
                    }
                    case Events.MessageEndEvent e -> {
                        if (e.content() != null) fullResponse.append(e.content());
                    }
                    default -> {}
                }
            });

            System.out.println("\n=== Agent Response ===");
            System.out.println(fullResponse);

            sdk.stop();
            System.out.println("\nSDK stopped");

            System.out.println("\n=== To use different loop strategies ===");
            System.out.println("Configure the CLI with appropriate flags or config:");
            System.out.println("  - ReAct (default): Standard reasoning loop");
            System.out.println("  - Plan-and-Execute: Plan first, then execute");
            System.out.println("  - Parallel: Execute tools in parallel");
            System.out.println("  - Reflexion: Self-reflective execution");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
