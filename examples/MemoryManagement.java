import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 08 Memory Management - Demonstrates agent memory persistence across sessions.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="MemoryManagement"
 */
public class MemoryManagement {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Autohand SDK Memory Management Example ===\n");

        // Phase 1: Save a preference to memory
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        AutohandSDK saveSdk = new AutohandSDK(new SDKConfig(
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

        saveSdk.start();
        System.out.println("SDK started (save session)\n");

        String savePrompt = "Save this to memory: \"The user prefers TypeScript over JavaScript and likes functional programming patterns.\"";
        streamPromptWithLogging(saveSdk, savePrompt);

        ContextUsage usageBefore = saveSdk.getContextUsage();
        System.out.println("Context usage before stop:");
        System.out.println("  memoryFiles: " + usageBefore.memoryFiles() + " files");
        System.out.println("  total:       " + usageBefore.total() + " tokens\n");

        saveSdk.stop();
        System.out.println("SDK stopped (save session)\n");

        // Phase 2: Start a fresh session and recall the memory
        AutohandSDK recallSdk = new AutohandSDK(new SDKConfig(
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

        recallSdk.start();
        System.out.println("SDK started (recall session)\n");

        String recallPrompt = "Recall what you know about my programming preferences from memory. What language do I prefer and what style do I like?";
        streamPromptWithLogging(recallSdk, recallPrompt);

        ContextUsage usageAfter = recallSdk.getContextUsage();
        System.out.println("Context usage after recall:");
        System.out.println("  memoryFiles: " + usageAfter.memoryFiles() + " files");
        System.out.println("  total:       " + usageAfter.total() + " tokens\n");

        recallSdk.stop();
        System.out.println("SDK stopped (recall session)");
    }

    static void streamPromptWithLogging(AutohandSDK sdk, String prompt) throws Exception {
        System.out.println("\n> " + prompt + "\n");
        StringBuilder fullResponse = new StringBuilder();

        sdk.streamPrompt(new PromptParams(prompt), event -> {
            switch (event) {
                case Events.ToolStartEvent e -> System.out.println("[Tool: " + e.toolName() + "]");
                case Events.ToolUpdateEvent e -> System.out.print(e.output());
                case Events.ToolEndEvent e -> {
                    System.out.println("\n[Tool completed: " + e.toolName() + "]");
                    if (e.output() != null) {
                        String preview = e.output().length() > 500
                            ? e.output().substring(0, 500) + "\n... (truncated)"
                            : e.output();
                        System.out.println(preview);
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

        System.out.println("\n");
    }
}
