import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Permission handling example.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="PermissionHandling"
 */
public class PermissionHandling {
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
            sdk.setPermissionMode(PermissionMode.INTERACTIVE);

            sdk.streamPrompt(new PromptParams("Run git status and summarize the changes."), event -> {
                if (event instanceof Events.PermissionRequestEvent pre) {
                    System.out.println("Permission request: " + pre.tool() + " - " + pre.description());
                    try {
                        // Allow this specific command for this session
                        sdk.allowPermission(pre.requestId(), DecisionScope.SESSION);
                    } catch (Exception e) {
                        System.err.println("Failed to respond: " + e.getMessage());
                    }
                } else if (event instanceof Events.MessageUpdateEvent mue) {
                    System.out.print(mue.delta());
                }
            });
        } finally {
            sdk.close();
        }
    }
}
