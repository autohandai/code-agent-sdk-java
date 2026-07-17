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
                        notify("autohand.autoresearch.status", Map.of(
                                "active", true,
                                "goal", "Improve SDK reliability",
                                "iteration", 1,
                                "maxIterations", 3,
                                "runsLogged", 1,
                                "statusText", "Autoresearch active",
                                "subcommand", "status",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.autoresearch.event", Map.of(
                                "operation", "history",
                                "phase", "completed",
                                "success", true,
                                "timestamp", Instant.now().toString()));
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
                        notify("autohand.turnEnd", Map.of(
                                "turnId", "turn-1",
                                "status", "completed",
                                "tokensUsed", 321,
                                "tokensUsageStatus", "actual",
                                "durationMs", 450,
                                "contextPercent", 0.42,
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
                            "autohand.reloadPlugins",
                            "autohand.mcp.toggleServer",
                            "autohand.mcp.reconnectServer",
                            "autohand.mcp.setServers",
                            "autohand.saveSession",
                            "autohand.resumeSession",
                            "autohand.hooks.removeHook",
                            "autohand.hooks.toggleHook" -> respond(id, Map.of("success", true));
                    case "autohand.applyFlagSettings" -> {
                        JsonNode features = request.path("params").path("settings").path("features");
                        if (features.path("slashGoal").asBoolean(false)
                                && features.path("tokenUsageStatus").asBoolean(false)) {
                            respond(id, Map.of("success", true));
                        } else {
                            respondError(id, -32602, "Expected slashGoal and tokenUsageStatus startup features");
                        }
                    }
                    case "autohand.getSupportedModels" -> respond(id, Map.of(
                            "models", List.of(Map.of(
                                    "id", "fantail",
                                    "displayName", "Fantail 2",
                                    "description", "Autohand default model",
                                    "provider", "autohandai"))));
                    case "autohand.getSupportedCommands" -> respond(id, Map.of(
                            "commands", List.of("help", "model", "hooks", "deep-research", "autoresearch", "goal")));
                    case "autohand.goal.get" -> respond(id, Map.of(
                            "version", 1,
                            "goal", goalState("goal-1", "Ship SDK parity", "active"),
                            "queue", List.of(),
                            "completed", List.of(),
                            "updatedAt", 1_721_171_200L));
                    case "autohand.goal.create" -> respond(id, Map.of(
                            "ok", true,
                            "goal", goalState("goal-2", request.path("params").path("objective").asText(), "active"),
                            "queue", List.of(),
                            "message", "tokenBudget=" + request.path("params").path("token_budget").asLong()));
                    case "autohand.goal.update" -> respond(id, Map.of(
                            "ok", true,
                            "goal", goalState("goal-2", "Ship SDK parity", request.path("params").path("status").asText()),
                            "queue", List.of(),
                            "message", "tokenCleared=" + request.path("params").path("token_budget").isNull()
                                    + ",timePresent=" + request.path("params").has("time_budget_seconds")));
                    case "autohand.goal.clear" -> respond(id, Map.of(
                            "ok", true, "queue", List.of(), "message", "cleared"));
                    case "autohand.goal.queue" -> respond(id, Map.of(
                            "ok", true, "queue", List.of(), "message", "queued:" + request.path("params").path("objective").asText()));
                    case "autohand.goal.startQueued" -> respond(id, Map.of(
                            "ok", true, "queue", List.of(), "message", "started"));
                    case "autohand.goal.listTemplates" -> respond(id, List.of(Map.of(
                            "name", "release",
                            "path", ".autohand/goals/release.md",
                            "description", "Ship a release",
                            "aliases", List.of("ship"),
                            "allowCommands", true,
                            "requiredPlaceholders", List.of(),
                            "requiredFlags", List.of("channel"),
                            "requiresArgs", false)));
                    case "autohand.autoresearch.start" -> {
                        notify("autohand.autoresearch.start", Map.of(
                                "active", true,
                                "goal", request.path("params").path("objective").asText(),
                                "iteration", 0,
                                "maxIterations", request.path("params").path("maxIterations").asInt(3),
                                "runsLogged", 0,
                                "statusText", "Autoresearch active",
                                "subcommand", "start",
                                "timestamp", Instant.now().toString()));
                        respond(id, Map.of(
                                "success", true,
                                "instruction", "Run the next autoresearch experiment",
                                "active", true,
                                "statusText", "Autoresearch active",
                                "runsLogged", 0));
                    }
                    case "autohand.autoresearch.status" -> respond(id, Map.of(
                            "success", true,
                            "active", true,
                            "statusText", "Autoresearch active",
                            "runsLogged", 1,
                            "paretoAttemptIds", List.of("attempt-1")));
                    case "autohand.autoresearch.stop" -> {
                        notify("autohand.autoresearch.pause", Map.of(
                                "active", false,
                                "runsLogged", 1,
                                "statusText", "Autoresearch paused",
                                "subcommand", "stop",
                                "timestamp", Instant.now().toString()));
                        respond(id, Map.of(
                                "success", true,
                                "active", false,
                                "statusText", "Autoresearch paused",
                                "runsLogged", 1));
                    }
                    case "autohand.autoresearch.history" -> {
                        notify("autohand.autoresearch.event", Map.of(
                                "operation", "history",
                                "phase", "completed",
                                "success", true,
                                "timestamp", Instant.now().toString()));
                        respond(id, Map.of(
                                "success", true,
                                "attempts", List.of(historyAttempt("attempt-1", "baseline"))));
                    }
                    case "autohand.autoresearch.replay" -> respond(id, Map.of(
                            "success", true,
                            "attemptId", request.path("params").path("attemptId").asText(),
                            "evaluatorMode", "original",
                            "metrics", Map.of("test_ms", 120.0),
                            "samples", List.of(),
                            "driftWarnings", List.of()));
                    case "autohand.autoresearch.rescore" -> respond(id, Map.of(
                            "success", true,
                            "decisions", List.of()));
                    case "autohand.autoresearch.compare" -> respond(id, Map.of(
                            "success", true,
                            "comparison", Map.of(
                                    "left", comparisonSide(request.path("params").path("leftAttemptId").asText()),
                                    "right", comparisonSide(request.path("params").path("rightAttemptId").asText()))));
                    case "autohand.autoresearch.pareto" -> respond(id, Map.of(
                            "success", true,
                            "attemptIds", List.of("attempt-1")));
                    case "autohand.autoresearch.pin" -> respond(id, Map.of(
                            "success", true,
                            "attemptId", request.path("params").path("attemptId").asText(),
                            "pinned", request.path("params").path("pinned").asBoolean()));
                    case "autohand.autoresearch.prune" -> respond(id, Map.of(
                            "success", true,
                            "applied", false,
                            "candidates", List.of(Map.of(
                                    "attemptId", "attempt-2",
                                    "objects", List.of("objects/patch.diff"),
                                    "bytes", 512,
                                    "protected", false,
                                    "reason", "retention policy")),
                            "bytesFreed", 0,
                            "remainingBytes", 512));
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
                            "model", "fantail"));
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

    private static void respondError(JsonNode id, int code, String message) throws Exception {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("jsonrpc", "2.0");
        node.set("id", id);
        node.set("error", MAPPER.valueToTree(Map.of("code", code, "message", message)));
        System.out.println(MAPPER.writeValueAsString(node));
        System.out.flush();
    }

    private static Map<String, Object> historyAttempt(String attemptId, String materialization) {
        return Map.of(
                "attemptId", attemptId,
                "description", "Baseline measurement",
                "timestamp", Instant.now().toString(),
                "legacy", false,
                "replayable", true,
                "pinned", false,
                "materialization", materialization);
    }

    private static Map<String, Object> comparisonSide(String attemptId) {
        return Map.of(
                "attemptId", attemptId,
                "samples", List.of(),
                "aggregates", Map.of(),
                "checks", Map.of("passed", true),
                "execution", Map.of("outcome", "passed"));
    }

    private static Map<String, Object> goalState(String goalId, String objective, String status) {
        return Map.of(
                "goalId", goalId,
                "objective", objective,
                "status", status,
                "tokenBudget", 10_000,
                "tokensUsed", 200,
                "timeUsedSeconds", 30,
                "createdAt", 1_721_171_100L,
                "updatedAt", 1_721_171_200L);
    }
}
