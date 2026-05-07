import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 22 SDLC workflow: release readiness review.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="SdlcReleaseReadiness"
 */
public class SdlcReleaseReadiness {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String model = System.getenv("AUTOHAND_MODEL");

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"), cliPath, false, 300_000,
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
        sdk.setSkills(java.util.List.of(
            new SkillReference("typescript", null, null),
            new SkillReference("testing", null, null)
        ));

        try {
            sdk.start();

            String prompt = String.join("\n", java.util.List.of(
                "Run a release-readiness pass for this Java SDK.",
                "Use the repository standard commands: mvn compile, mvn test, mvn package.",
                "If a command fails, stop and explain the failure with file references.",
                "If all commands pass, summarize residual risks and production readiness."
            ));
            streamPrompt(sdk, prompt);
        } finally {
            sdk.stop();
        }
    }

    static void streamPrompt(AutohandSDK sdk, String message) throws Exception {
        List<String> toolResults = new ArrayList<>();

        sdk.streamPrompt(new PromptParams(message), event -> {
            switch (event) {
                case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                case Events.ToolStartEvent e -> System.out.println("\n[tool:start] " + e.toolName());
                case Events.ToolUpdateEvent e -> System.out.print(e.output());
                case Events.ToolEndEvent e -> {
                    toolResults.add(e.toolName() + ": " + (e.success() ? "pass" : "fail"));
                    System.out.println("[tool:end] " + e.toolName() + " success=" + e.success());
                }
                case Events.PermissionRequestEvent e -> System.out.println("\n[permission] " + e.tool() + ": " + e.description());
                case Events.ErrorEvent e -> System.err.println("\n[error] " + e.message());
                default -> {}
            }
        });

        if (!toolResults.isEmpty()) {
            System.out.println("\n--- tool summary ---");
            toolResults.forEach(System.out::println);
        }
    }
}
