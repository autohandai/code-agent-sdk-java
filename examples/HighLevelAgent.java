import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.RunResult;
import ai.autohand.sdk.types.*;

/**
 * High-level Agent API example.
 * This is the recommended API for application code.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="HighLevelAgent"
 */
public class HighLevelAgent {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String model = System.getenv("AUTOHAND_MODEL");

        Agent agent = Agent.create(AgentOptions.builder()
            .cwd(".")
            .cliPath(cliPath)
            .model(model)
            .instructions("You are reviewing a Java SDK API.\nPrefer small, typed, composable interfaces.\nCall out permission-sensitive work before recommending execution.")
            .build());

        try {
            var run = agent.send("Review the public SDK API and list the next three production hardening tasks.");

            run.stream(event -> {
                switch (event) {
                    case Events.MessageUpdateEvent mue -> System.out.print(mue.delta());
                    case Events.PermissionRequestEvent pre -> {
                        System.out.println("\n[permission] " + pre.tool() + ": " + pre.description());
                        try {
                            agent.denyPermission(pre.requestId(), DecisionScope.ONCE);
                        } catch (Exception e) {
                            System.err.println("Failed to deny permission: " + e.getMessage());
                        }
                    }
                    case Events.ToolStartEvent tse -> System.out.println("\n[tool] " + tse.toolName());
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
