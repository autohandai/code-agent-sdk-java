import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 04 Bash Command - Agent that runs shell commands.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="BashCommand"
 */
public class BashCommand {
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

            System.out.println("Sending prompt: What is the current directory listing and total file count?\n");

            StringBuilder fullResponse = new StringBuilder();
            sdk.streamPrompt(new PromptParams("What is the current directory listing and total file count?"), event -> {
                switch (event) {
                    case Events.ToolStartEvent e -> System.out.println("[Tool called: " + e.toolName() + "]");
                    case Events.ToolEndEvent e -> {
                        System.out.println("[Tool completed: " + e.toolName() + "]");
                        if (e.output() != null) {
                            String preview = e.output().length() > 1000
                                ? e.output().substring(0, 1000) + "\n... (truncated)"
                                : e.output();
                            System.out.println("Output:\n" + preview);
                        }
                    }
                    case Events.PermissionRequestEvent e -> {
                        System.out.println("[Permission request: " + e.tool() + "]");
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
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
