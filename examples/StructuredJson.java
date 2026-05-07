import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.types.SDKConfig;

import java.util.List;
import java.util.Map;

/**
 * Structured JSON output example.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="StructuredJson"
 */
public class StructuredJson {

    public record ReleaseRisk(
        String summary,
        List<Risk> risks
    ) {}

    public record Risk(
        String title,
        String severity
    ) {}

    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        Agent agent = Agent.create(AgentOptions.builder()
            .cwd(".")
            .cliPath(cliPath)
            .build());

        try {
            var schema = Map.of(
                "summary", "string",
                "risks", List.of(Map.of("title", "string", "severity", "low | medium | high"))
            );

            ReleaseRisk risk = agent.runJson(
                "Assess the publish readiness of this codebase.",
                ReleaseRisk.class,
                "ReleaseRisk",
                schema,
                null
            );

            System.out.println("Summary: " + risk.summary());
            for (Risk r : risk.risks()) {
                System.out.println("  - " + r.title() + " [" + r.severity() + "]");
            }
        } finally {
            agent.close();
        }
    }
}
