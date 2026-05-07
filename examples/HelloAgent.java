import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * 01 Hello Agent - Simple example of using the Autohand Java SDK.
 *
 * Prerequisites:
 * - Autohand CLI must be installed and available in PATH
 * - CLI must be authenticated (run `autohand login` first)
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="HelloAgent"
 */
public class HelloAgent {
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

            System.out.println("Sending prompt: Tell me a good joke about code AI agents!\n");
            System.out.println("=== Agent Response ===\n");

            StringBuilder fullResponse = new StringBuilder();
            sdk.streamPrompt(new PromptParams("Tell me a good joke about code AI agents!"), event -> {
                switch (event) {
                    case Events.MessageUpdateEvent mue -> {
                        System.out.print(mue.delta());
                        fullResponse.append(mue.delta());
                    }
                    case Events.MessageEndEvent mee -> {
                        if (mee.content() != null) {
                            System.out.println(mee.content());
                            fullResponse.setLength(0);
                            fullResponse.append(mee.content());
                        }
                    }
                    case Events.ErrorEvent ee -> System.err.println("Error: " + ee.message());
                    default -> {}
                }
            });

            System.out.println("\n=== Full Response ===");
            System.out.println(fullResponse);

            GetStateResult state = sdk.getState();
            System.out.println("\n=== Agent State ===");
            System.out.println(state);

            sdk.stop();
            System.out.println("\nSDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
