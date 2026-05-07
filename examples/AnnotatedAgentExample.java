import ai.autohand.sdk.annotations.*;
import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.RunResult;
import ai.autohand.sdk.types.*;

/**
 * Annotated Agent Example - Demonstrates the annotation-based API.
 *
 * Define an agent class with annotations and let {@link AnnotatedAgentFactory}
 * wire everything together.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="AnnotatedAgentExample"
 */
@AutohandAgent(
    model = "fantail2",
    instructions = "You are a concise code reviewer. Prefer small, typed, composable interfaces."
)
@Skills({"typescript", "testing"})
@Permission(ai.autohand.sdk.types.PermissionMode.INTERACTIVE)
@EnableTools({Tool.READ_FILE, Tool.RUN_COMMAND, Tool.GIT_STATUS})
@AgentsMd(path = "auto")
public class AnnotatedAgentExample {

    public static void main(String[] args) throws Exception {
        // Create and start agent from annotations
        Agent agent = AnnotatedAgentFactory.create(AnnotatedAgentExample.class);

        try {
            var run = agent.send("Review the public SDK API and list the next three production hardening tasks.");

            run.stream(event -> {
                switch (event) {
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.PermissionRequestEvent e -> {
                        System.out.println("\n[permission] " + e.tool() + ": " + e.description());
                        try {
                            agent.denyPermission(e.requestId(), DecisionScope.ONCE);
                        } catch (Exception ex) {
                            System.err.println("Failed to deny permission: " + ex.getMessage());
                        }
                    }
                    case Events.ToolStartEvent e -> System.out.println("\n[tool] " + e.toolName());
                    default -> {}
                }
            });

            RunResult result = run.waitForResult();
            System.out.println("\n\nRun " + result.id() + " " + result.status() + " with " + result.events().size() + " events.");
        } finally {
            agent.close();
        }
    }
}
