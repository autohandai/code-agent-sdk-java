import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.StructuredOutputError;
import ai.autohand.sdk.types.*;

import java.util.List;
import java.util.Map;

/**
 * JSON Mode Example - Structured JSON output with validation.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="JsonModeExample"
 */
public class JsonModeExample {

    public record ReleaseRisk(String summary, List<Risk> risks) {}
    public record Risk(String title, String severity, String mitigation) {}

    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        Agent agent = Agent.create(AgentOptions.builder()
            .cwd(".")
            .cliPath(cliPath)
            .instructions("Prefer concise, factual release-readiness analysis.")
            .build());

        try {
            System.out.println("Agent started. Requesting structured JSON...\n");

            var schema = Map.of(
                "summary", "string",
                "risks", List.of(Map.of(
                    "title", "string",
                    "severity", "low | medium | high",
                    "mitigation", "string"
                ))
            );

            String prompt = String.join("\n", List.of(
                "Assess this SDK repository for publish readiness. Do not execute commands.",
                "",
                "Return only valid JSON. Do not wrap the response in Markdown.",
                "The JSON value should satisfy: ReleaseRisk.",
                "Use this JSON schema or example shape:",
                new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(schema),
                "If you cannot inspect the repository, still return a JSON object.",
                "Use summary to explain the limitation and set risks to an empty array if no risks can be assessed."
            ));

            var run = agent.send(prompt);

            run.stream(event -> {
                switch (event) {
                    case Events.AgentStartEvent e -> System.out.println("[agent] " + e.sessionId() + " using " + e.model());
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.MessageEndEvent e -> {
                        if (e.content() != null && !e.content().isEmpty()) System.out.println();
                    }
                    case Events.ToolStartEvent e -> System.out.println("\n[tool] " + e.toolName());
                    case Events.PermissionRequestEvent e -> {
                        System.out.println("\n[permission] " + e.tool() + ": " + e.description());
                        try {
                            agent.denyPermission(e.requestId(), DecisionScope.ONCE);
                        } catch (Exception ex) {
                            System.err.println("Failed to deny: " + ex.getMessage());
                        }
                    }
                    default -> {}
                }
            });

            ReleaseRisk result = run.json(ReleaseRisk.class);

            System.out.println("\n\nParsed JSON:");
            System.out.println(new com.fasterxml.jackson.databind.ObjectMapper()
                .writerWithDefaultPrettyPrinter().writeValueAsString(result));

        } catch (StructuredOutputError e) {
            System.err.println("Structured output error: " + e.getMessage());
            System.err.println("Raw response preview: " + e.getRawResponsePreview());
            System.exit(1);
        } finally {
            agent.close();
        }
    }
}
