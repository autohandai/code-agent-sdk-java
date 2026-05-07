import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Streaming example for Autohand SDK.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="Streaming"
 */
public class Streaming {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"), cliPath, true, 300_000,
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
            sdk.start();
            System.out.println("SDK started");

            sdk.streamPrompt(new PromptParams("Analyze the current directory structure"), event -> {
                switch (event) {
                    case Events.AgentStartEvent e -> System.out.println("[Agent] Started: " + e.sessionId());
                    case Events.TurnStartEvent e -> System.out.println("[Turn] Started: " + e.turnId());
                    case Events.MessageStartEvent e -> System.out.println("[Message] Started: " + e.messageId());
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.MessageEndEvent e -> System.out.println("\n[Message] Completed");
                    case Events.ToolStartEvent e -> System.out.println("[Tool] " + e.toolName() + " started");
                    case Events.ToolEndEvent e -> {
                        System.out.println("[Tool] " + e.toolName() + " completed: " + (e.success() ? "success" : "failed"));
                        if (e.output() != null) {
                            String preview = e.output().length() > 500
                                ? e.output().substring(0, 500) + "\n  ... (truncated)"
                                : e.output();
                            System.out.println("  Output: " + preview);
                        }
                    }
                    case Events.AgentEndEvent e -> System.out.println("[Agent] Ended: " + e.reason());
                    case Events.ErrorEvent e -> System.err.println("[Error] " + e.message());
                    default -> {}
                }
            });

            sdk.stop();
            System.out.println("SDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
