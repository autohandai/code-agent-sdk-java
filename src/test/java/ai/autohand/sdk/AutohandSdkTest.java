package ai.autohand.sdk;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.ContextUsage;
import ai.autohand.sdk.types.DecisionScope;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.HookDefinition;
import ai.autohand.sdk.types.HookEvent;
import ai.autohand.sdk.types.HookResultTypes;
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
            assertTrue(events.stream().anyMatch(Events.AgentEndEvent.class::isInstance));
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
            sdk.setModel("fantail2");
            sdk.setMaxThinkingTokens(1_000);

            List<ModelInfo> models = sdk.supportedModels();
            ContextUsage usage = sdk.getContextUsage();
            HookResultTypes.AddHookResult hook = sdk.addHook(
                    new HookDefinition(HookEvent.POST_TOOL, "echo ok", true, null, 5));

            assertEquals("fantail2", models.getFirst().id());
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
                .model("fantail2")
                .appendSystemPrompt("Prefer Java examples.")
                .skills(List.of(new SkillReference("java", null, null), new SkillReference(null, "./skills/release/SKILL.md", null)))
                .autoMode(true)
                .contextCompact(false)
                .addDirectory("/tmp/fixtures")
                .build();

        List<String> args = config.cliArgs();

        assertTrue(args.contains("--mode"));
        assertTrue(args.contains("rpc"));
        assertTrue(args.contains("--model"));
        assertTrue(args.contains("fantail2"));
        assertTrue(args.contains("--append-sys-prompt"));
        assertTrue(args.contains("--skills"));
        assertTrue(args.contains("java,./skills/release/SKILL.md"));
        assertTrue(args.contains("--auto-mode"));
        assertTrue(args.contains("--no-context-compact"));
        assertTrue(args.contains("--add-dir"));
        assertTrue(args.contains("/tmp/fixtures"));
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
