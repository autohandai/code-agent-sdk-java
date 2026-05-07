import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Hooks Example - Configure and use CLI hooks.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="HooksExample"
 */
public class HooksExample {
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

            // Add a hook that fires on tool execution
            HookDefinition hook = new HookDefinition(
                HookEvent.POST_TOOL,
                "echo \"Tool completed: {{toolName}}\"",
                true,
                new HookFilter(null, null),
                0
            );

            HookResultTypes.AddHookResult result = sdk.addHook(hook);
            System.out.println("Added hook: " + result.message());

            // List all hooks
            HookResultTypes.GetHooksResult hooks = sdk.getHooks();
            System.out.println("Active hooks: " + hooks.hooks().size());

            // Run a prompt to see hooks in action
            sdk.streamPrompt(new PromptParams("List files in current directory"), event -> {
                switch (event) {
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.ToolEndEvent e -> System.out.println("\n[Hook would fire after: " + e.toolName() + "]");
                    default -> {}
                }
            });

            sdk.stop();
            System.out.println("\nSDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
