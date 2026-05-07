import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Session Resume Example - Save and resume agent sessions.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="SessionResume"
 */
public class SessionResume {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String sessionId;

        // Phase 1: Start a session, send a message, save it
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
            System.out.println("SDK started (phase 1)\n");

            SessionMetadata meta = sdk.getSessionMetadata();
            sessionId = meta.sessionId();
            System.out.println("Session ID: " + sessionId);

            sdk.streamPrompt(new PromptParams("Remember that my favorite programming language is Java."), event -> {
                if (event instanceof Events.MessageUpdateEvent e) System.out.print(e.delta());
            });
            System.out.println();

            sdk.saveSession();
            System.out.println("\nSession saved");

            sdk.stop();
            System.out.println("SDK stopped (phase 1)\n");
        } catch (Exception e) {
            System.err.println("Error in phase 1: " + e.getMessage());
            sdk.stop();
            throw e;
        }

        // Phase 2: Resume the session and verify memory
        AutohandSDK resumed = new AutohandSDK(new SDKConfig(
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
            resumed.resumeSession(sessionId);
            System.out.println("SDK started (phase 2 - resumed)\n");
            System.out.println("Resumed session: " + sessionId);

            resumed.streamPrompt(new PromptParams("What is my favorite programming language?"), event -> {
                if (event instanceof Events.MessageUpdateEvent e) System.out.print(e.delta());
            });
            System.out.println();

            resumed.stop();
            System.out.println("\nSDK stopped (phase 2)");
        } catch (Exception e) {
            System.err.println("Error in phase 2: " + e.getMessage());
            resumed.stop();
            throw e;
        }
    }
}
