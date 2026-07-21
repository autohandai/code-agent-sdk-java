package ai.autohand.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class FakeAutohandCli {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final CountDownLatch CONTROL_RPC_RECEIVED = new CountDownLatch(1);

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
                        String requestedMessage = request.path("params").path("message").asText();
                        if (requestedMessage.equals("control-concurrency")) {
                            startControlConcurrencyPrompt(id.deepCopy());
                            continue;
                        }
                        String responseText = requestedMessage.startsWith("concurrent-")
                                ? requestedMessage : "hello from java";
                        if (requestedMessage.equals("malformed-feature-event")) {
                            notifyMalformedFeatureEvents();
                        }
                        if (requestedMessage.equals("out-of-range-hook-integers")) {
                            notifyOutOfRangeHookIntegers();
                        }
                        notify("autohand.automode.iteration", Map.of(
                                "sessionId", "auto-session",
                                "iteration", 3,
                                "actions", List.of("edit", "test"),
                                "tokensUsed", 1200,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.automode.complete", Map.of(
                                "sessionId", "auto-session",
                                "iterations", 3,
                                "filesCreated", 2,
                                "filesModified", 5,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.automode.error", Map.of(
                                "sessionId", "auto-session-failed",
                                "error", "Iteration failed",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.preTool", Map.of(
                                "toolId", "tool-call-1",
                                "toolName", "read_file",
                                "args", Map.of("path", "README.md"),
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.postTool", Map.of(
                                "toolId", "tool-call-1",
                                "toolName", "read_file",
                                "success", true,
                                "duration", 18,
                                "output", "contents",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.prePrompt", Map.of(
                                "instruction", "Review the SDK",
                                "mentionedFiles", List.of("README.md", "pom.xml"),
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.postResponse", Map.of(
                                "tokensUsed", 640,
                                "tokensUsageStatus", "actual",
                                "toolCallsCount", 2,
                                "duration", 250,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.fileModified", Map.of(
                                "filePath", "src/Main.java",
                                "changeType", "create",
                                "toolId", "tool-call-1",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.sessionError", Map.of(
                                "error", "Rate limited",
                                "code", "RATE_LIMIT",
                                "context", Map.of("retryAfter", 60),
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.stop", Map.of(
                                "tokensUsed", 700,
                                "tokensUsageStatus", "unavailable",
                                "toolCallsCount", 3,
                                "duration", 300.5,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.sessionStart", Map.of(
                                "sessionType", "resume",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.sessionEnd", Map.of(
                                "reason", "clear",
                                "duration", 450.5,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.subagentStop", Map.of(
                                "subagentId", "subagent-1",
                                "subagentName", "reviewer",
                                "subagentType", "code-review",
                                "success", false,
                                "duration", 75.5,
                                "error", "Review failed",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.permissionRequest", Map.of(
                                "tool", "write_file",
                                "path", "README.md",
                                "command", "write README.md",
                                "args", Map.of("content", "updated"),
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.notification", Map.of(
                                "notificationType", "warning",
                                "message", "Context is nearly full",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.contextCompacted", Map.of(
                                "croppedCount", 4,
                                "summary", "Earlier turns summarized",
                                "usagePercent", 0.6125,
                                "reason", "threshold",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.contextOverflow", Map.of(
                                "tokensBefore", 120_000,
                                "tokensAfter", 80_000,
                                "croppedCount", 6,
                                "usagePercent", 1.05,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.contextWarning", Map.of(
                                "usagePercent", 0.805,
                                "remainingTokens", 12_000,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.hook.contextCritical", Map.of(
                                "usagePercent", 0.9575,
                                "remainingTokens", 3_000,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.mcp.invokeRequest", Map.of(
                                "requestId", "mcp-invoke-1",
                                "toolName", "vscode__github__search",
                                "args", Map.of("query", "sdk"),
                                "timestamp", Instant.now().toString()));
                        notify("autohand.mcp.toolsChanged", Map.of(
                                "tools", List.of(Map.of(
                                        "name", "vscode__github__search",
                                        "description", "Search issues",
                                "serverName", "github")),
                                "timestamp", Instant.now().toString()));
                        notify("autohand.learn.progress", Map.of(
                                "status", "loading-registry",
                                "timestamp", Instant.now().toString()));
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
                                "delta", responseText,
                                "timestamp", Instant.now().toString()));
                        notify("autohand.customFutureEvent", Map.of(
                                "meaning", "kept for forward compatibility",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.messageUpdate", Map.of(
                                "messageId", "msg-1",
                                "delta", "",
                                "timestamp", Instant.now().toString()));
                        notify("autohand.messageEnd", Map.of(
                                "messageId", "msg-1",
                                "content", responseText,
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
                    case "autohand.permissionAcknowledged" -> {
                        JsonNode params = request.path("params");
                        respond(id, Map.of("success", params.size() == 1
                                && "permission-1".equals(params.path("requestId").asText())));
                    }
                    case "autohand.directoryAccessResponse" -> {
                        JsonNode params = request.path("params");
                        respond(id, Map.of("success", params.size() == 2
                                && "directory-1".equals(params.path("requestId").asText())
                                && params.path("granted").asBoolean()));
                    }
                    case "autohand.directoryAccessAcknowledged" -> {
                        JsonNode params = request.path("params");
                        respond(id, Map.of("success", params.size() == 1
                                && "directory-1".equals(params.path("requestId").asText())));
                    }
                    case "autohand.changesDecision" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 3
                                && "batch-1".equals(params.path("batchId").asText())
                                && "accept_selected".equals(params.path("action").asText())
                                && params.path("selectedChangeIds").size() == 1
                                && "change-1".equals(params.path("selectedChangeIds").get(0).asText());
                        respond(id, Map.of(
                                "success", exact,
                                "appliedCount", exact ? 1 : 0,
                                "skippedCount", exact ? 1 : 2,
                                "errors", List.of()));
                    }
                    case "autohand.getHistory" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 2
                                && params.path("page").asInt() == 2
                                && params.path("pageSize").asInt() == 25;
                        respond(id, Map.of(
                                "sessions", List.of(Map.of(
                                        "sessionId", "session-history-1",
                                        "createdAt", "2026-07-20T00:00:00Z",
                                        "lastActiveAt", "2026-07-20T00:10:00Z",
                                        "projectName", "java-sdk",
                                        "model", "fantail",
                                        "messageCount", exact ? 7 : -1,
                                        "status", "completed")),
                                "currentPage", 2,
                                "totalPages", 3,
                                "totalItems", 51));
                    }
                    case "autohand.getSession" -> {
                        JsonNode params = request.path("params");
                        String sessionId = params.path("sessionId").asText();
                        if (params.size() != 1 || "missing-session".equals(sessionId)) {
                            respond(id, Map.of("success", false, "error", "Session not found"));
                        } else {
                            respond(id, Map.ofEntries(
                                    Map.entry("success", true),
                                    Map.entry("sessionId", sessionId),
                                    Map.entry("projectName", "java-sdk"),
                                    Map.entry("model", "fantail"),
                                    Map.entry("messageCount", 1),
                                    Map.entry("status", "completed"),
                                    Map.entry("createdAt", "2026-07-20T00:00:00Z"),
                                    Map.entry("lastActiveAt", "2026-07-20T00:10:00Z"),
                                    Map.entry("summary", "Session summary"),
                                    Map.entry("messages", List.of(Map.of(
                                            "id", "message-1",
                                            "role", "assistant",
                                            "content", "done",
                                            "timestamp", "2026-07-20T00:10:00Z"))),
                                    Map.entry("workspaceRoot", "/workspace")));
                        }
                    }
                    case "autohand.session.attach" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 1
                                && "session-attach-1".equals(params.path("sessionId").asText());
                        respond(id, Map.of(
                                "success", exact,
                                "sessionId", "session-attach-1",
                                "workspaceRoot", "/workspace",
                                "messageCount", 9));
                    }
                    case "autohand.yoloSet" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 2
                                && "*".equals(params.path("pattern").asText())
                                && params.path("timeoutSeconds").asInt() == 60;
                        respond(id, Map.of("success", exact, "expiresIn", 60));
                    }
                    case "autohand.yolo.set" -> {
                        JsonNode params = request.path("params");
                        respond(id, Map.of("success", params.size() == 1
                                && params.path("pattern").asText().isEmpty()));
                    }
                    case "autohand.mcp.setVscodeTools" -> {
                        JsonNode tool = request.path("params").path("tools").path(0);
                        boolean exact = request.path("params").size() == 1
                                && request.path("params").path("tools").size() == 1
                                && "search".equals(tool.path("name").asText())
                                && "Search issues".equals(tool.path("description").asText())
                                && "github".equals(tool.path("serverName").asText())
                                && "object".equals(tool.path("inputSchema").path("type").asText())
                                && tool.path("inputSchema").path("required").size() == 1;
                        respond(id, Map.of("success", exact));
                    }
                    case "autohand.mcp.invokeResponse" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 3
                                && "mcp-request-1".equals(params.path("requestId").asText())
                                && params.path("success").asBoolean()
                                && "issue-42".equals(params.path("result").asText())
                                && !params.has("error");
                        respond(id, Map.of("success", exact));
                    }
                    case "autohand.learn.recommend" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 1 && params.path("deep").asBoolean();
                        respond(id, Map.of(
                                "success", exact,
                                "projectSummary", "Java SDK project",
                                "audit", List.of(Map.of(
                                        "skill", "legacy-java",
                                        "status", "outdated",
                                        "reason", "Uses Java 17")),
                                "recommendations", List.of(Map.of(
                                        "slug", "java-21",
                                        "score", 0.98,
                                        "reason", "Uses records")),
                                "gapAnalysis", "Add virtual-thread guidance"));
                    }
                    case "autohand.learn.update" -> respond(id, Map.of(
                            "success", request.path("params").isEmpty(),
                            "updated", 1,
                            "unchanged", 1,
                            "results", List.of(
                                    Map.of("name", "java-21", "status", "updated"),
                                    Map.of("name", "testing", "status", "unchanged"))));
                    case "autohand.learn.generate" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 1
                                && "project".equals(params.path("scope").asText());
                        respond(id, Map.of(
                                "success", exact,
                                "skillName", "java-sdk-learning",
                                "skillPath", ".agents/skills/java-sdk-learning"));
                    }
                    case "autohand.getToolsRegistry" -> respond(id, Map.of(
                            "tools", List.of(Map.ofEntries(
                                    Map.entry("name", "read_file"),
                                    Map.entry("description", "Read a file"),
                                    Map.entry("requiresApproval", false),
                                    Map.entry("source", "builtin"),
                                    Map.entry("scope", "project"),
                                    Map.entry("disabled", false),
                                    Map.entry("schemaVersion", 1),
                                    Map.entry("reuseHint", "Reuse read results"))),
                            "diagnostics", request.path("params").isEmpty()
                                    ? List.of(Map.of("file", "broken-tool.json", "reason", "Invalid schema"))
                                    : List.of()));
                    case "autohand.setContextCompact" -> {
                        JsonNode params = request.path("params");
                        respond(id, Map.of("enabled", params.size() == 1
                                && params.path("enabled").asBoolean()));
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
                            "active", request.path("params").isEmpty(),
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
                    case "autohand.getState" -> {
                        CONTROL_RPC_RECEIVED.countDown();
                        respond(id, Map.of(
                                "status", "running",
                                "sessionId", "session-1",
                                "model", "fantail"));
                    }
                    case "autohand.getMessages" -> respond(id, Map.of("messages", List.of(Map.of(
                            "id", "msg-1",
                            "role", "assistant",
                            "content", "hello from java",
                            "timestamp", "2026-07-20T00:00:00Z",
                            "toolCalls", List.of(Map.of(
                                    "id", "call-1", "name", "read_file", "args", Map.of("path", "README.md")))))));
                    case "autohand.reset" -> respond(id, Map.of(
                            "sessionId", request.path("params").isEmpty() ? "reset-session" : "unexpected-params"));
                    case "autohand.browserHandoff.create" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 2
                                && params.path("extensionId").asText().equals("extension-1")
                                && params.path("installUrl").asText().equals("https://example.test/install");
                        respond(id, Map.of(
                                "token", exact ? "handoff-token" : "unexpected-params",
                                "sessionId", "browser-session",
                                "workspaceRoot", "/workspace",
                                "createdAt", "2026-07-20T00:00:00Z",
                                "expiresAt", "2026-07-20T00:05:00Z",
                                "url", "https://example.test/handoff"));
                    }
                    case "autohand.browserHandoff.attach" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 1
                                && params.path("token").asText().equals("handoff-token");
                        respond(id, Map.of(
                                "success", exact,
                                "sessionId", "browser-session",
                                "workspaceRoot", "/workspace",
                                "messageCount", 3));
                    }
                    case "autohand.browserHandoff.attachLatest" -> respond(id, Map.of(
                            "success", request.path("params").isEmpty(),
                            "sessionId", "latest-session",
                            "workspaceRoot", "/workspace",
                            "messageCount", 5));
                    case "autohand.automode.start" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 7
                                && params.path("prompt").asText().equals("Ship the SDK")
                                && params.path("maxIterations").asInt() == 8
                                && params.path("completionPromise").asText().equals("DONE")
                                && params.path("useWorktree").asBoolean()
                                && params.path("checkpointInterval").asInt() == 2
                                && params.path("maxRuntime").asInt() == 600
                                && params.path("maxCost").asDouble() == 4.5;
                        respond(id, Map.of("success", exact, "sessionId", "auto-session"));
                    }
                    case "autohand.automode.status" -> respond(id, Map.of(
                            "active", true,
                            "paused", false,
                            "state", Map.of(
                                    "sessionId", "auto-session",
                                    "status", "running",
                                    "currentIteration", 2,
                                    "maxIterations", 8,
                                    "filesCreated", 1,
                                    "filesModified", 3,
                                    "branch", "autohand/auto-session",
                                    "lastCheckpoint", Map.of(
                                            "commit", "checkpoint-1",
                                            "message", "iteration 2",
                                            "timestamp", "2026-07-20T00:02:00Z"))));
                    case "autohand.automode.pause" -> respond(id, Map.of(
                            "success", request.path("params").isEmpty()));
                    case "autohand.automode.resume" -> respond(id, Map.of(
                            "success", request.path("params").isEmpty()));
                    case "autohand.automode.cancel" -> {
                        JsonNode params = request.path("params");
                        respond(id, Map.of(
                                "success", params.size() == 1
                                        && params.path("reason").asText().equals("Operator requested stop")));
                    }
                    case "autohand.automode.getLog" -> {
                        JsonNode params = request.path("params");
                        boolean exact = params.size() == 1 && params.path("limit").asInt() == 25;
                        respond(id, Map.of(
                                "success", exact,
                                "iterations", List.of(Map.of(
                                        "iteration", 2,
                                        "timestamp", "2026-07-20T00:02:00Z",
                                        "actions", List.of("edit", "test"),
                                        "tokensUsed", 1200,
                                        "cost", 0.08,
                                        "checkpoint", Map.of(
                                                "commit", "checkpoint-1",
                                                "message", "iteration 2")))));
                    }
                    case "autohand.getSkillsRegistry" -> respond(id, Map.of(
                            "success", true,
                            "skills", List.of(Map.of(
                                    "id", "java-quality",
                                    "name", "Java Quality",
                                    "description", "Review Java code",
                                    "category", "development",
                                    "tags", List.of("java", "review"),
                                    "rating", 4.8,
                                    "downloadCount", 120,
                                    "isFeatured", true,
                                    "isCurated", true)),
                            "categories", List.of(Map.of("name", "development", "count", 1))));
                    case "autohand.installSkill" -> respond(id, Map.of(
                            "success", true,
                            "skillName", request.path("params").path("skillName").asText(),
                            "path", request.path("params").path("scope").asText().equals("project")
                                    ? ".agents/skills/java-quality" : ".autohand/skills/java-quality"));
                    case "autohand.mcp.listServers" -> respond(id, Map.of(
                            "servers", List.of(Map.of("name", "github", "status", "connected", "toolCount", 2))));
                    case "autohand.mcp.listTools" -> respond(id, Map.of(
                            "tools", List.of(Map.of(
                                    "name", "get_issue",
                                    "description", "Get a GitHub issue",
                                    "serverName", request.path("params").path("serverName").asText("github")))));
                    case "autohand.mcp.getServerConfigs" -> respond(id, Map.of(
                            "configs", List.of(Map.of(
                                    "name", "github",
                                    "transport", "stdio",
                                    "command", "github-mcp",
                                    "args", List.of("serve"),
                                    "env", Map.of("LOG_LEVEL", "info"),
                                    "autoConnect", true))));
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
                    case "autohand.test.hang" -> {
                        // Intentionally wait for the SDK to close the transport.
                    }
                    case "autohand.test.late" -> startDelayedResponse(id.deepCopy());
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

    private static void notifyMalformedFeatureEvents() throws Exception {
        String timestamp = Instant.now().toString();
        notify("autohand.automode.iteration", Map.ofEntries(
                Map.entry("sessionId", "auto-session"),
                Map.entry("iteration", "three"),
                Map.entry("actions", List.of("edit")),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.automode.iteration")));
        notify("autohand.automode.complete", Map.ofEntries(
                Map.entry("sessionId", "auto-session"),
                Map.entry("iterations", 3),
                Map.entry("filesCreated", 2),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.automode.complete")));
        notify("autohand.automode.error", Map.ofEntries(
                Map.entry("sessionId", "auto-session"),
                Map.entry("error", 42),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.automode.error")));
        notify("autohand.hook.preTool", Map.ofEntries(
                Map.entry("toolId", "tool-call-1"),
                Map.entry("toolName", "read_file"),
                Map.entry("args", List.of("README.md")),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.preTool")));
        notify("autohand.hook.postTool", Map.ofEntries(
                Map.entry("toolId", "tool-call-1"),
                Map.entry("toolName", "read_file"),
                Map.entry("success", "yes"),
                Map.entry("duration", 18),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.postTool")));
        notify("autohand.hook.prePrompt", Map.ofEntries(
                Map.entry("instruction", "Review the SDK"),
                Map.entry("mentionedFiles", List.of(42)),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.prePrompt")));
        notify("autohand.hook.postResponse", Map.ofEntries(
                Map.entry("tokensUsed", 640),
                Map.entry("tokensUsageStatus", "estimated"),
                Map.entry("toolCallsCount", 2),
                Map.entry("duration", 250),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.postResponse")));
        notify("autohand.hook.fileModified", Map.ofEntries(
                Map.entry("filePath", "src/Main.java"),
                Map.entry("changeType", "renamed"),
                Map.entry("toolId", "tool-call-1"),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.fileModified")));
        notify("autohand.hook.sessionError", Map.ofEntries(
                Map.entry("error", 42),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.sessionError")));
        notify("autohand.hook.stop", Map.ofEntries(
                Map.entry("tokensUsed", 700),
                Map.entry("tokensUsageStatus", "estimated"),
                Map.entry("toolCallsCount", 3),
                Map.entry("duration", 300.5),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.stop")));
        notify("autohand.hook.sessionStart", Map.ofEntries(
                Map.entry("sessionType", "restart"),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.sessionStart")));
        notify("autohand.hook.sessionEnd", Map.ofEntries(
                Map.entry("reason", "timeout"),
                Map.entry("duration", 450.5),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.sessionEnd")));
        notify("autohand.hook.subagentStop", Map.ofEntries(
                Map.entry("subagentId", "subagent-1"),
                Map.entry("subagentName", "reviewer"),
                Map.entry("subagentType", "code-review"),
                Map.entry("success", "yes"),
                Map.entry("duration", 75.5),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.subagentStop")));
        notify("autohand.hook.permissionRequest", Map.ofEntries(
                Map.entry("tool", "write_file"),
                Map.entry("args", List.of("README.md")),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.permissionRequest")));
        notify("autohand.hook.notification", Map.ofEntries(
                Map.entry("notificationType", "warning"),
                Map.entry("message", 42),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.notification")));
        notify("autohand.hook.contextCompacted", Map.ofEntries(
                Map.entry("croppedCount", -1),
                Map.entry("usagePercent", 0.6125),
                Map.entry("reason", "threshold"),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.contextCompacted")));
        notify("autohand.hook.contextOverflow", Map.ofEntries(
                Map.entry("tokensBefore", -1),
                Map.entry("tokensAfter", 80_000),
                Map.entry("croppedCount", 6),
                Map.entry("usagePercent", 1.05),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.contextOverflow")));
        notify("autohand.hook.contextWarning", Map.ofEntries(
                Map.entry("usagePercent", 0.805),
                Map.entry("remainingTokens", -1),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.contextWarning")));
        notify("autohand.hook.contextCritical", Map.ofEntries(
                Map.entry("usagePercent", -0.01),
                Map.entry("remainingTokens", 3_000),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.hook.contextCritical")));
        notify("autohand.mcp.invokeRequest", Map.ofEntries(
                Map.entry("requestId", "mcp-invoke-1"),
                Map.entry("toolName", "vscode__github__search"),
                Map.entry("args", List.of("sdk")),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.mcp.invokeRequest")));
        notify("autohand.mcp.toolsChanged", Map.ofEntries(
                Map.entry("tools", List.of(Map.of(
                        "name", "vscode__github__search",
                        "description", "Search issues"))),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.mcp.toolsChanged")));
        notify("autohand.learn.progress", Map.ofEntries(
                Map.entry("status", "unknown"),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "autohand.learn.progress")));
    }

    private static void notifyOutOfRangeHookIntegers() throws Exception {
        String timestamp = Instant.now().toString();
        notify("autohand.hook.postResponse", Map.ofEntries(
                Map.entry("tokensUsed", new BigInteger("9223372036854775808")),
                Map.entry("tokensUsageStatus", "actual"),
                Map.entry("toolCallsCount", 2),
                Map.entry("duration", 250),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "post-response-out-of-range")));
        notify("autohand.hook.stop", Map.ofEntries(
                Map.entry("tokensUsed", 700),
                Map.entry("tokensUsageStatus", "unavailable"),
                Map.entry("toolCallsCount", new BigInteger("2147483648")),
                Map.entry("duration", 300.5),
                Map.entry("timestamp", timestamp),
                Map.entry("malformedMarker", "stop-out-of-range")));
    }

    private static void startControlConcurrencyPrompt(JsonNode id) {
        Thread.ofVirtual().name("fake-control-concurrency-prompt").start(() -> {
            try {
                notify("autohand.messageUpdate", Map.of(
                        "messageId", "msg-control",
                        "delta", "control-ready",
                        "timestamp", Instant.now().toString()));
                if (!CONTROL_RPC_RECEIVED.await(5, TimeUnit.SECONDS)) {
                    respondError(id, -32000, "Timed out waiting for a concurrent control RPC");
                    return;
                }
                notify("autohand.messageEnd", Map.of(
                        "messageId", "msg-control",
                        "content", "control-ready",
                        "timestamp", Instant.now().toString()));
                respond(id, Map.of("success", true));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static void startDelayedResponse(JsonNode id) {
        Thread.ofVirtual().name("fake-delayed-response").start(() -> {
            try {
                Thread.sleep(250);
                respond(id, Map.of("success", true));
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
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
