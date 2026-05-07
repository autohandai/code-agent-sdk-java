import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 02 Streaming Query - See the agent response as it arrives.
 *
 * Demonstrates real-time event streaming with exhaustive pattern matching.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="StreamingQuery"
 */
public class StreamingQuery {

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
            sdk.start();
            System.out.println("SDK started\n");

            System.out.println("Streaming response:\n");
            sdk.streamPrompt(new PromptParams("Explain closures in one sentence"), StreamingQuery::handleEvent);

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
                System.out.println("\n[Agent started: " + e.sessionId() + "]");
                System.out.println("  Model: " + e.model());
            }
            case Events.TurnStartEvent e -> System.out.println("\n[Turn started: " + e.turnId() + "]");
            case Events.MessageUpdateEvent e -> {
                if (e.delta() != null) System.out.print(e.delta());
            }
            case Events.MessageEndEvent e -> {
                if (e.content() != null) System.out.println("\n[Message completed]");
            }
            case Events.ToolStartEvent e -> System.out.println("\n[Tool called: " + e.toolName() + "]");
            case Events.ToolUpdateEvent e -> System.out.print(e.output());
            case Events.ToolEndEvent e -> {
                System.out.println("\n[Tool completed: " + e.toolName() + "]");
                if (e.output() != null) {
                    String preview = e.output().length() > 500 ? e.output().substring(0, 500) + "... (truncated)" : e.output();
                    System.out.println("  Output: " + preview);
                }
            }
            case Events.PermissionRequestEvent e -> {
                System.out.println("\n[Permission request: " + e.tool() + "]");
                System.out.println("  Description: " + e.description());
            }
            case Events.TurnEndEvent e -> System.out.println("\n[Turn ended]");
            case Events.AgentEndEvent e -> System.out.println("\n[Agent ended]");
            case Events.ErrorEvent e -> System.err.println("\n[Error: " + e.message() + "]");
            default -> {}
        }
    }
}
