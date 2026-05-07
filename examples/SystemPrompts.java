import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 23 System prompt configuration.
 *
 * Use appendSystemPrompt for normal SDK integrations. Use setSystemPrompt only
 * when you intentionally own the complete agent contract.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="SystemPrompts"
 *   AUTOHAND_PROMPT_MODE=replace AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="SystemPrompts"
 */
public class SystemPrompts {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String model = System.getenv("AUTOHAND_MODEL");
        boolean replaceMode = "replace".equals(System.getenv("AUTOHAND_PROMPT_MODE"));

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            ".", cliPath, false, 300_000,
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

        if (replaceMode) {
            sdk.setSystemPrompt(String.join("\n", java.util.List.of(
                "You are Autohand Code operating as a release-review agent.",
                "Inspect the repository carefully.",
                "Return concise findings with file references and verification steps."
            )));
        } else {
            sdk.appendSystemPrompt(String.join("\n", java.util.List.of(
                "For this SDK repository, prefer Maven commands.",
                "Call out permission-sensitive operations before recommending execution.",
                "Keep responses focused on Java SDK API design."
            )));
        }

        try {
            sdk.start();

            sdk.streamPrompt(new PromptParams("Review the public SDK surface for system prompt ergonomics."), event -> {
                switch (event) {
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.PermissionRequestEvent e -> {
                        System.out.println("\n[permission] " + e.tool() + ": " + e.description());
                        try {
                            sdk.denyPermission(e.requestId(), DecisionScope.ONCE);
                        } catch (Exception ex) {
                            System.err.println("Failed to deny: " + ex.getMessage());
                        }
                    }
                    case Events.AgentEndEvent e -> {}
                    default -> {}
                }
            });
        } finally {
            sdk.close();
        }
    }
}
