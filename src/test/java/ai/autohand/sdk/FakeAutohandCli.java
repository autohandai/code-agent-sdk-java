package ai.autohand.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FakeAutohandCli {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FakeAutohandCli() {
    }

    public static void main(String[] args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode request = MAPPER.readTree(line);
                String method = request.path("method").asText();
                JsonNode id = request.get("id");

                switch (method) {
                    case "autohand.prompt" -> {
                        notify("autohand.permissionRequest", Map.of(
                                "requestId", "perm-1",
                                "tool", "bash",
                                "description", "Run a safe test command",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.messageStart", Map.of(
                                "messageId", "msg-1",
                                "role", "assistant",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.messageUpdate", Map.of(
                                "messageId", "msg-1",
                                "delta", "hello ",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.customFutureEvent", Map.of(
                                "meaning", "kept for forward compatibility",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.messageUpdate", Map.of(
                                "messageId", "msg-1",
                                "delta", "from java",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.messageEnd", Map.of(
                                "messageId", "msg-1",
                                "content", "hello from java",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.agentEnd", Map.of(
                                "sessionId", "session-1",
                                "reason", "completed",
                                "timestamp", Instant.now().toString()));
                        respond(id, Map.of("success", true));
                    }
                    case "autohand.permissionResponse",
                            "autohand.permissionModeSet",
                            "autohand.planModeSet",
                            "autohand.modelSet",
                            "autohand.maxThinkingTokensSet",
                            "autohand.applyFlagSettings",
                            "autohand.reloadPlugins",
                            "autohand.mcp.toggleServer",
                            "autohand.mcp.reconnectServer",
                            "autohand.mcp.setServers",
                            "autohand.saveSession",
                            "autohand.resumeSession",
                            "autohand.hooks.removeHook",
                            "autohand.hooks.toggleHook" -> respond(id, Map.of("success", true));
                    case "autohand.getSupportedModels" -> respond(id, Map.of(
                            "models", List.of(Map.of(
                                    "id", "fantail2",
                                    "displayName", "Fantail 2",
                                    "description", "Autohand default model",
                                    "provider", "autohandai"))));
                    case "autohand.getSupportedCommands" -> respond(id, Map.of("commands", List.of("help", "model", "hooks")));
                    case "autohand.getContextUsage" -> respond(id, Map.of(
                            "systemPrompt", 10,
                            "tools", 11,
                            "messages", 12,
                            "mcpTools", 3,
                            "memoryFiles", 6,
                            "total", 42));
                    case "autohand.getAccountInfo" -> respond(id, Map.of(
                            "email", "user@example.com",
                            "organization", "Autohand",
                            "subscriptionType", "developer"));
                    case "autohand.getState" -> respond(id, Map.of(
                            "status", "running",
                            "sessionId", "session-1",
                            "model", "fantail2"));
                    case "autohand.getMessages" -> respond(id, Map.of("messages", List.of("hello from java")));
                    case "autohand.hooks.addHook" -> respond(id, Map.of(
                            "success", true,
                            "hookId", "hook-1",
                            "message", "Hook added"));
                    case "autohand.hooks.getHooks" -> respond(id, Map.of(
                            "settings", Map.of(
                                    "enabled", true,
                                    "hooks", List.of(Map.of(
                                            "event", "post-tool",
                                            "command", "echo ok",
                                            "enabled", true,
                                            "timeoutSeconds", 5)))));
                    default -> respond(id, Map.of("success", true, "method", method));
                }
            }
        }
    }

    private static void notify(String method, Object params) throws Exception {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.put("method", method);
        node.set("params", MAPPER.valueToTree(params));
        System.out.println(MAPPER.writeValueAsString(node));
        System.out.flush();
    }

    private static void respond(JsonNode id, Object result) throws Exception {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.set("id", id);
        node.set("result", MAPPER.valueToTree(result));
        System.out.println(MAPPER.writeValueAsString(node));
        System.out.flush();
    }
}
