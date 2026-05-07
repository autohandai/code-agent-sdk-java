import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Basic usage example for the Autohand Java SDK.
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass="BasicUsage" -Dexec.classpathScope=compile
 */
public class BasicUsage {
    public static void main(String[] args) throws Exception {
        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"),
            System.getenv("AUTOHAND_CLI_PATH"),
            false,
            300_000,
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

            System.out.println("Sending prompt: Hello, Autohand!");
            sdk.streamPrompt(new PromptParams("Hello, Autohand!"), event -> {
                switch (event) {
                    case Events.MessageUpdateEvent mue -> System.out.print(mue.delta());
                    case Events.MessageEndEvent mee -> System.out.println();
                    default -> {}
                }
            });

            System.out.println("Prompt completed");

            GetStateResult state = sdk.getState();
            System.out.println("Current state: " + state.status());

            GetMessagesResult messages = sdk.getMessages();
            System.out.println("Messages: " + messages.messages().size());

            sdk.stop();
            System.out.println("SDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
