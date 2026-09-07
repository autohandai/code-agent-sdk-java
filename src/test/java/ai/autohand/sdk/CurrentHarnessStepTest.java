package ai.autohand.sdk;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.types.PromptParams;
import ai.autohand.sdk.types.SDKConfig;
import ai.autohand.sdk.types.StopConditions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CurrentHarnessStepTest {
    @TempDir Path directory;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void autohandAIStopsAfterPersistedResultsAndResumesThroughTheActualCli() throws Exception {
        String cli = System.getenv("AUTOHAND_TEST_CLI_PATH");
        assumeTrue(cli != null && !cli.isBlank(), "Set AUTOHAND_TEST_CLI_PATH to test the actual CLI");
        List<JsonNode> requests = new CopyOnWriteArrayList<>();
        var serverFailure = new AtomicReference<Throwable>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Object response;
                if (exchange.getRequestURI().getPath().equals("/auth/me")) {
                    assertEquals("Bearer session-token", exchange.getRequestHeaders().getFirst("Authorization"));
                    response = Map.of("user", Map.of("id", "java-sdk", "email", "sdk@example.test"));
                } else {
                    assertEquals("/v1/chat/completions", exchange.getRequestURI().getPath());
                    assertEquals("Bearer inference-key", exchange.getRequestHeaders().getFirst("Authorization"));
                    JsonNode request = MAPPER.readTree(exchange.getRequestBody());
                    assertEquals("fantail", request.path("model").textValue());
                    requests.add(request);
                    boolean first = requests.size() == 1;
                    Object message = first ? Map.of("role", "assistant", "content", "Read evidence", "tool_calls", List.of(
                            Map.of("id", "read-evidence", "type", "function", "function", Map.of("name", "read_file",
                                    "arguments", "{\"path\":\"evidence.txt\"}"))))
                            : Map.of("role", "assistant", "content", "continued from persisted evidence");
                    response = Map.of("id", "java-completion", "choices", List.of(Map.of("message", message,
                                    "finish_reason", first ? "tool_calls" : "stop")),
                            "usage", Map.of("prompt_tokens", 10, "completion_tokens", 5, "total_tokens", 15));
                }
                byte[] body = MAPPER.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (Throwable failure) {
                serverFailure.set(failure);
                byte[] body = "Unexpected local provider request".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, body.length);
                exchange.getResponseBody().write(body);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            Path workspace = Files.createDirectory(directory.resolve("workspace"));
            Path home = Files.createDirectory(directory.resolve("home"));
            Path config = home.resolve("config.json");
            Files.writeString(workspace.resolve("evidence.txt"), "java-persisted-marker");
            JsonNode saved = MAPPER.valueToTree(Map.of(
                    "provider", "openai", "auth", Map.of("token", "session-token"),
                    "openai", Map.of("apiKey", "saved-key", "baseUrl", "http://127.0.0.1:1/unused", "model", "saved-model"),
                    "features", Map.of("automaticSpecialists", false),
                    "agent", Map.of("autoMemory", false), "telemetry", Map.of("enabled", false)));
            Files.writeString(config, MAPPER.writeValueAsString(saved));
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var options = SDKConfig.builder().cwd(workspace.toString()).cliPath(cli)
                    .provider("autohandai").apiKey("inference-key").baseUrl(base + "/v1").model("fantail")
                    .bare(true).unrestricted(true).timeoutMs(30_000)
                    .environment(Map.of("AUTOHAND_HOME", home.toString(), "AUTOHAND_CONFIG", config.toString(),
                            "AUTOHAND_AUTH_API_URL", base + "/auth", "AUTOHAND_SKIP_PING", "1",
                            "AUTOHAND_SKIP_UPDATE_CHECK", "1", "AUTOHAND_NO_IDLE_LOGOUT", "1",
                            "AUTOHAND_DISABLE_AUTO_REPORT", "1")).build();
            try (var agent = Agent.create(options)) {
                assertTrue(agent.sdk().supportedAgents().stream().anyMatch(a -> a.name().equals("reviewer") && a.source().equals("builtin")));
                var result = agent.send(new PromptParams("Read evidence.txt using read_file")
                        .withStopWhen(StopConditions.isStepCount(1))).waitForResult();
                assertEquals("stopped", result.status());
                assertEquals(1, result.steps().size());
                assertEquals(1, requests.size());
                assertTrue(result.steps().getFirst().toolResults().getFirst().output().contains("java-persisted-marker"));
                var next = agent.run("Continue using the saved tool result");
                assertEquals("completed", next.status());
                assertEquals("continued from persisted evidence", next.text());
                assertEquals(2, requests.size());
                assertTrue(requests.getLast().path("messages").toString().contains("java-persisted-marker"));
                JsonNode persisted = MAPPER.readTree(config.toFile());
                assertEquals(saved.path("provider"), persisted.path("provider"));
                assertEquals(saved.path("openai"), persisted.path("openai"));
                assertFalse(persisted.has("autohandai"));
            }
            assertNull(serverFailure.get());
        } finally {
            server.stop(0);
        }
    }
}
