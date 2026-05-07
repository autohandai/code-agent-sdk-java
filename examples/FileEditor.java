import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 05 File Editor Agent - Agent that reads and edits files.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="FileEditor"
 */
public class FileEditor {
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

            String prompt = "Read README.md and fix any obvious typos in comments.";
            System.out.println("Sending prompt: \"" + prompt + "\"\n");

            StringBuilder fullResponse = new StringBuilder();
            sdk.streamPrompt(new PromptParams(prompt), event -> {
                switch (event) {
                    case Events.ToolStartEvent e -> System.out.println("[Tool called: " + e.toolName() + "]");
                    case Events.ToolUpdateEvent e -> System.out.print(e.output());
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
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
