import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 13 Permissions - Demonstrating permission modes.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="PermissionsDemo"
 */
public class PermissionsDemo {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        PermissionMode[] modes = {PermissionMode.INTERACTIVE, PermissionMode.RESTRICTED, PermissionMode.UNRESTRICTED};

        try {
            System.out.println("=== Permission Modes Demo ===\n");

            for (PermissionMode mode : modes) {
                System.out.println("\n--- Testing " + mode + " mode ---");

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

                sdk.start();
                sdk.setPermissionMode(mode);
                System.out.println("SDK started with permission mode: " + mode);

                String prompt = "List the files in the current directory";
                System.out.println("\nSending prompt: \"" + prompt + "\"");

                try {
                    StringBuilder fullResponse = new StringBuilder();
                    sdk.streamPrompt(new PromptParams(prompt), event -> {
                        switch (event) {
                            case Events.ToolStartEvent e -> System.out.println("\n[Tool called: " + e.toolName() + "]");
                            case Events.ToolEndEvent e -> {
                                System.out.println("\n[Tool completed: " + e.toolName() + "]");
                                if (e.output() != null) {
                                    String preview = e.output().length() > 500
                                        ? e.output().substring(0, 500) + "\n  ... (truncated)"
                                        : e.output();
                                    System.out.println("  Output: " + preview);
                                }
                            }
                            case Events.PermissionRequestEvent e -> {
                                System.out.println("\n[Permission request: " + e.tool() + "]");
                                System.out.println("  Description: " + e.description());
                                System.out.println("  Request ID: " + e.requestId());
                                if (mode == PermissionMode.INTERACTIVE) {
                                    System.out.println("  Auto-approving for demo...");
                                    try {
                                        sdk.allowPermission(e.requestId(), DecisionScope.ONCE);
                                    } catch (Exception ex) {
                                        System.err.println("Failed to allow: " + ex.getMessage());
                                    }
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
                    System.out.println("\nPrompt completed");
                } catch (Exception e) {
                    System.out.println("\nPrompt failed: " + e.getMessage());
                }

                sdk.stop();
                System.out.println("SDK stopped");
            }

            System.out.println("\n=== Demo Complete ===");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
