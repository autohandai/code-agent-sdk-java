package ai.autohand.sdk;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.ContextUsage;
import ai.autohand.sdk.types.Autoresearch;
import ai.autohand.sdk.types.DecisionScope;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.HookDefinition;
import ai.autohand.sdk.types.HookEvent;
import ai.autohand.sdk.types.HookResultTypes;
import ai.autohand.sdk.types.Goals;
import ai.autohand.sdk.types.FeatureFlagSettings;
import ai.autohand.sdk.types.SkillSource;
import ai.autohand.sdk.types.ModelInfo;
import ai.autohand.sdk.types.PermissionMode;
import ai.autohand.sdk.types.PromptParams;
import ai.autohand.sdk.types.SDKConfig;
import ai.autohand.sdk.types.SkillReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutohandSdkTest {
    @TempDir
    Path tempDir;

    @Test
    void streamsPromptEventsThroughRealJsonRpcTransport() throws Exception {
        SDKConfig config = SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .debug(false)
                .timeoutMs(10_000)
                .build();

        try (AutohandSDK sdk = new AutohandSDK(config)) {
            sdk.start();

            List<Event> events = new ArrayList<>();
            StringBuilder text = new StringBuilder();
            sdk.streamPrompt(new PromptParams("hello"), event -> {
                events.add(event);
                if (event instanceof Events.PermissionRequestEvent permission) {
                    sdk.allowPermission(permission.requestId(), DecisionScope.ONCE);
                }
                if (event instanceof Events.MessageUpdateEvent update) {
                    text.append(update.delta());
                }
            });

            assertEquals("hello from java", text.toString());
            assertTrue(events.stream().anyMatch(Events.PermissionRequestEvent.class::isInstance));
            assertTrue(events.stream().anyMatch(Events.AutoresearchLifecycleEvent.class::isInstance));
            assertTrue(events.stream().anyMatch(Events.AutoresearchOperationEvent.class::isInstance));
            assertTrue(events.stream().anyMatch(Events.AgentEndEvent.class::isInstance));
            Events.TurnEndEvent turnEnd = events.stream()
                    .filter(Events.TurnEndEvent.class::isInstance)
                    .map(Events.TurnEndEvent.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(321L, turnEnd.tokensUsed());
            assertEquals("actual", turnEnd.tokensUsageStatus());
            assertEquals(0.42, turnEnd.contextPercent());
        }
    }

    @Test
    void highLevelAgentRunsAndCollectsResultText() throws Exception {
        try (Agent agent = Agent.create(AgentOptions.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .instructions("Be concise.")
                .permissionMode(PermissionMode.INTERACTIVE)
                .skills(List.of(new SkillReference("java", null, null)))
                .build())) {

            var result = agent.run("Say hello");

            assertEquals("completed", result.status());
            assertEquals("hello from java", result.text());
            assertFalse(result.events().isEmpty());
        }
    }

    @Test
    void controlMethodsUseCliRpcMethods() throws Exception {
        try (AutohandSDK sdk = new AutohandSDK(SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .build())) {
            sdk.start();

            sdk.setPermissionMode(PermissionMode.INTERACTIVE);
            sdk.enablePlanMode();
            sdk.setModel("fantail");
            sdk.setMaxThinkingTokens(1_000);

            List<ModelInfo> models = sdk.supportedModels();
            ContextUsage usage = sdk.getContextUsage();
            HookResultTypes.AddHookResult hook = sdk.addHook(
                    new HookDefinition(HookEvent.POST_TOOL, "echo ok", true, null, 5));

            assertEquals("fantail", models.getFirst().id());
            assertEquals("autohandai", models.getFirst().provider());
            assertEquals(42, usage.total());
            assertEquals("Hook added", hook.message());
            assertEquals(1, sdk.getHooks().hooks().size());
            assertEquals("user@example.com", sdk.accountInfo().email());
            assertEquals("running", sdk.getState().status());
        }
    }

    @Test
    void configBuilderProducesCliFlagsHumansExpect() {
        SDKConfig config = SDKConfig.builder()
                .cwd("/workspace")
                .model("fantail")
                .appendSystemPrompt("Prefer Java examples.")
                .skills(List.of(new SkillReference("java", null, null), new SkillReference(null, "./skills/release/SKILL.md", null)))
                .autoMode(true)
                .contextCompact(false)
                .addDirectory("/tmp/fixtures")
                .bare(true)
                .idleLogout(false)
                .fork("session-123")
                .displayLanguage("en")
                .systemPromptFile("./SYSTEM.md")
                .appendSystemPromptFile("./APPEND.md")
                .mcpConfig("./mcp.json")
                .agents("./agents")
                .pluginDir("./plugins")
                .sessionPath("./sessions")
                .autoSaveInterval(15)
                .agentsMdEnabled(true)
                .agentsMdCreate(true)
                .agentsMdPath("./AGENTS.md")
                .agentsMdAutoUpdate(true)
                .maxTokens(100_000)
                .compressionThreshold(0.75)
                .summarizationThreshold(0.9)
                .skillSources(List.of(SkillSource.CODEX_USER, SkillSource.AUTOHAND_PROJECT))
                .installMissingSkills(true)
                .build();

        List<String> args = config.cliArgs();

        assertTrue(args.contains("--mode"));
        assertTrue(args.contains("rpc"));
        assertTrue(args.contains("--model"));
        assertTrue(args.contains("fantail"));
        assertTrue(args.contains("--append-sys-prompt"));
        assertTrue(args.contains("--skills"));
        assertTrue(args.contains("java,./skills/release/SKILL.md"));
        assertTrue(args.contains("--auto-mode"));
        assertTrue(args.contains("--no-context-compact"));
        assertTrue(args.contains("--add-dir"));
        assertTrue(args.contains("/tmp/fixtures"));
        assertTrue(args.contains("--bare"));
        assertTrue(args.contains("--no-idle-logout"));
        assertTrue(args.containsAll(List.of(
                "--fork", "session-123", "--display-language", "en",
                "--system-prompt-file", "./SYSTEM.md", "--append-system-prompt-file", "./APPEND.md",
                "--mcp-config", "./mcp.json", "--agents", "./agents", "--plugin-dir", "./plugins")));
        assertTrue(args.containsAll(List.of(
                "--session-path", "./sessions", "--auto-save-interval", "15",
                "--agents-md", "--agents-md-create", "--agents-md-path", "./AGENTS.md",
                "--agents-md-auto-update", "--max-tokens", "100000",
                "--compression-threshold", "0.75", "--summarization-threshold", "0.9",
                "--skill-sources", "codex-user,autohand-project", "--install-missing-skills")));
    }

    @Test
    void autohandAiProviderMapsCredentialsToCliEnvironment() {
        SDKConfig config = SDKConfig.builder()
                .provider("autohandai")
                .apiKey("secret-test-key")
                .baseUrl("https://api.example.test")
                .autohandAIPlan("local")
                .build();

        assertEquals("secret-test-key", config.environment().get("AUTOHAND_AI_API_KEY"));
        assertEquals("https://api.example.test", config.environment().get("AUTOHAND_AI_BASE_URL"));
        assertEquals("local", config.environment().get("AUTOHAND_AI_PLAN"));
    }

    @Test
    void exposesAllTypedPersistentGoalOperationsWithTriStateUpdates() throws Exception {
        FeatureFlagSettings features = FeatureFlagSettings.builder()
                .slashGoal(true)
                .tokenUsageStatus(true)
                .build();
        try (AutohandSDK sdk = new AutohandSDK(SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .features(features)
                .build())) {
            sdk.start();

            assertEquals("goal-1", sdk.getGoal().snapshot().goal().goalId());
            Goals.MutationResult created = sdk.createGoal(new Goals.CreateParams(
                    "Ship SDK parity", new Goals.Budget(10_000L, 3_600L, null, null)));
            assertEquals("tokenBudget=10000", created.message());

            Goals.UpdateParams update = new Goals.UpdateParams(
                    null,
                    Goals.Status.PAUSED,
                    Goals.NullableUpdate.clear(),
                    Goals.NullableUpdate.unchanged(),
                    Goals.NullableUpdate.set(500L),
                    Goals.NullableUpdate.unchanged());
            assertEquals("tokenCleared=true,timePresent=false", sdk.updateGoal(update).message());
            assertEquals("queued:Publish release notes", sdk.queueGoal(new Goals.CreateParams("Publish release notes")).message());
            assertEquals("started", sdk.startQueuedGoal().message());
            assertEquals("release", sdk.listGoalTemplates().templates().getFirst().name());
            assertEquals("cleared", sdk.clearGoal().message());
            assertTrue(sdk.supportsCommand("/goal"));
        }
    }

    @Test
    void exposesTypedReplayableAutoresearchLifecycle() throws Exception {
        try (AutohandSDK sdk = new AutohandSDK(SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .build())) {
            sdk.start();

            assertTrue(sdk.supportsCommand("/autoresearch"));
            Autoresearch.StartParams params = Autoresearch.StartParams.builder("Reduce test runtime")
                    .metricName("test_ms")
                    .metricUnit("ms")
                    .direction(Autoresearch.OptimizationDirection.LOWER)
                    .measureCommand("mvn test")
                    .maxIterations(3)
                    .sampling(new Autoresearch.SamplingOptions(3, 9, 2.0))
                    .build();

            assertTrue(sdk.startAutoresearch(params).success());
            assertEquals("Run the next autoresearch experiment", sdk.startAutoresearch(params).instruction());
            assertEquals(1, sdk.getAutoresearchStatus().runsLogged());
            assertEquals("attempt-1", sdk.getAutoresearchHistory().attempts().getFirst().attemptId());
            assertEquals(120.0, sdk.replayAutoresearch(new Autoresearch.ReplayParams(
                    "attempt-1", Autoresearch.EvaluatorMode.ORIGINAL)).metrics().get("test_ms"));
            assertTrue(sdk.rescoreAutoresearch(Autoresearch.RescoreParams.attempt("attempt-1")).success());
            assertTrue(sdk.compareAutoresearch(new Autoresearch.CompareParams("attempt-1", "attempt-0")).success());
            assertEquals(List.of("attempt-1"), sdk.getAutoresearchPareto().attemptIds());
            assertTrue(sdk.pinAutoresearch(new Autoresearch.PinParams("attempt-1", true)).pinned());
            assertEquals(512, sdk.pruneAutoresearch(Autoresearch.PruneParams.preview()).remainingBytes());
            assertFalse(sdk.stopAutoresearch().active());
        }
    }

    @Test
    void exposesCurrentAutoresearchHookNames() {
        assertEquals("autoresearch:decision", HookEvent.AUTORESEARCH_DECISION.toCliString());
        assertEquals("autoresearch:replay", HookEvent.AUTORESEARCH_REPLAY.toCliString());
        assertEquals("autoresearch:rescore", HookEvent.AUTORESEARCH_RESCORE.toCliString());
        assertEquals("autoresearch:prune", HookEvent.AUTORESEARCH_PRUNE.toCliString());
        assertEquals("goal-written:completed", HookEvent.GOAL_WRITTEN_COMPLETED.toCliString());
    }

    @Test
    void unknownNotificationsStayInspectable() throws Exception {
        try (AutohandSDK sdk = new AutohandSDK(SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .build())) {
            sdk.start();

            List<Event> events = new ArrayList<>();
            sdk.streamPrompt(new PromptParams("unknown"), events::add);

            Event unknown = events.stream()
                    .filter(Events.UnknownEvent.class::isInstance)
                    .findFirst()
                    .orElseThrow();
            assertInstanceOf(Events.UnknownEvent.class, unknown);
        }
    }

    private Path fakeCli() throws Exception {
        Path script = tempDir.resolve("autohand-fake-cli");
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");
        Files.writeString(script, """
                #!/bin/sh
                exec "%s" -cp "%s" ai.autohand.sdk.FakeAutohandCli "$@"
                """.formatted(java, classpath));
        script.toFile().setExecutable(true);
        return script;
    }
}
