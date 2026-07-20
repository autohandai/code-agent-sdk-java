package ai.autohand.sdk;

import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.PermissionAcknowledgement;
import ai.autohand.sdk.types.DirectoryAccessResponse;
import ai.autohand.sdk.types.DirectoryAccessAcknowledgement;
import ai.autohand.sdk.types.ChangesDecision;
import ai.autohand.sdk.types.SessionHistory;
import ai.autohand.sdk.types.SessionDetails;
import ai.autohand.sdk.types.SessionAttachment;
import ai.autohand.sdk.types.YoloMode;
import ai.autohand.sdk.types.VscodeMcpTools;
import ai.autohand.sdk.types.McpInvocationResponse;
import ai.autohand.sdk.types.LearnRecommendation;
import ai.autohand.sdk.types.LearnUpdate;
import ai.autohand.sdk.types.LearnGeneration;
import ai.autohand.sdk.types.ToolsRegistry;
import ai.autohand.sdk.types.ContextCompaction;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.PromptParams;
import ai.autohand.sdk.types.SDKConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SdkControlE2ETest {
    @TempDir
    Path tempDir;

    @Test
    void acknowledgesPermissionThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            PermissionAcknowledgement.Result result = sdk.acknowledgePermission("permission-1");

            assertTrue(result.success());
        }
    }

    @Test
    void respondsToDirectoryAccessThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            DirectoryAccessResponse.Result result = sdk.respondDirectoryAccess("directory-1", true);

            assertTrue(result.success());
        }
    }

    @Test
    void acknowledgesDirectoryAccessThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            DirectoryAccessAcknowledgement.Result result = sdk.acknowledgeDirectoryAccess("directory-1");

            assertTrue(result.success());
        }
    }

    @Test
    void decidesSelectedChangesThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            ChangesDecision.Result result = sdk.decideChanges(new ChangesDecision.Params(
                    "batch-1", ChangesDecision.Action.ACCEPT_SELECTED, List.of("change-1")));

            assertTrue(result.success());
            assertTrue(result.errors().isEmpty());
        }
    }

    @Test
    void getsTypedSessionHistoryThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            SessionHistory.Result result = sdk.getHistory(new SessionHistory.Params(2, 25));

            assertTrue(result.sessions().getFirst().status() == SessionHistory.Status.COMPLETED);
            assertTrue(result.sessions().getFirst().messageCount() == 7);
        }
    }

    @Test
    void getsDiscriminatedSessionDetailsThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            SessionDetails.Success loaded = assertInstanceOf(
                    SessionDetails.Success.class, sdk.getSession("session-details-1"));
            SessionDetails.Failure missing = assertInstanceOf(
                    SessionDetails.Failure.class, sdk.getSession("missing-session"));

            assertEquals("done", loaded.messages().getFirst().content());
            assertEquals("Session not found", missing.error());
        }
    }

    @Test
    void attachesSessionThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            SessionAttachment.Result result = sdk.attachSession("session-attach-1");

            assertTrue(result.success());
            assertEquals(9, result.messageCount());
        }
    }

    @Test
    void setsTimedYoloModeAndSupportsDottedAliasThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            YoloMode.Result enabled = sdk.setYoloMode(new YoloMode.Params("*", 60));
            YoloMode.Result disabledViaAlias = sdk.setYoloModeAlias(new YoloMode.Params("", null));

            assertTrue(enabled.success());
            assertEquals(60, enabled.expiresIn());
            assertTrue(disabledViaAlias.success());
        }
    }

    @Test
    void registersVscodeMcpToolsThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            VscodeMcpTools.InputSchema schema = VscodeMcpTools.InputSchema.object(
                    Map.of("query", Map.of("type", "string")), List.of("query"));
            VscodeMcpTools.Result result = sdk.setVscodeMcpTools(new VscodeMcpTools.Params(List.of(
                    new VscodeMcpTools.Tool("search", "Search issues", "github", schema))));

            assertTrue(result.success());
        }
    }

    @Test
    void respondsToMcpInvocationThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            McpInvocationResponse.Result result = sdk.respondMcpInvocation(
                    new McpInvocationResponse.Params("mcp-request-1", true, "issue-42", null));

            assertTrue(result.success());
        }
    }

    @Test
    void getsProjectLearningRecommendationsThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            LearnRecommendation.Result result = sdk.recommendLearn(new LearnRecommendation.Params(true));

            assertTrue(result.success());
            assertEquals(LearnRecommendation.AuditStatus.OUTDATED, result.audit().getFirst().status());
            assertEquals("java-21", result.recommendations().getFirst().slug());
        }
    }

    @Test
    void updatesProjectLearningThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            LearnUpdate.Result result = sdk.updateLearn();

            assertTrue(result.success());
            assertEquals(LearnUpdate.Status.UPDATED, result.results().getFirst().status());
            assertEquals(1, result.unchanged());
        }
    }

    @Test
    void generatesSkillFromProjectLearningThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            LearnGeneration.Result result = sdk.generateLearn(LearnGeneration.Scope.PROJECT);

            assertTrue(result.success());
            assertEquals("java-sdk-learning", result.skillName());
        }
    }

    @Test
    void getsTypedToolsRegistryThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            ToolsRegistry.Result result = sdk.getToolsRegistry();

            assertEquals(ToolsRegistry.Source.BUILTIN, result.tools().getFirst().source());
            assertEquals(ToolsRegistry.Scope.PROJECT, result.tools().getFirst().scope());
            assertEquals("Invalid schema", result.diagnostics().getFirst().reason());
        }
    }

    @Test
    void setsContextCompactionThroughSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            ContextCompaction.Result result = sdk.setContextCompact(true);

            assertTrue(result.enabled());
        }
    }

    @Test
    void streamsTypedAutoModeIterationEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            List<Event> events = streamFeatureEvents(sdk);
            Events.AutoModeIterationEvent event = events.stream()
                    .filter(Events.AutoModeIterationEvent.class::isInstance)
                    .map(Events.AutoModeIterationEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals(List.of("edit", "test"), event.actions());
            assertEquals(1200L, event.tokensUsed());
        }
    }

    @Test
    void streamsTypedAutoModeCompletionEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.AutoModeCompleteEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.AutoModeCompleteEvent.class::isInstance)
                    .map(Events.AutoModeCompleteEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals(3, event.iterations());
            assertEquals(5, event.filesModified());
        }
    }

    @Test
    void streamsTypedAutoModeErrorEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.AutoModeErrorEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.AutoModeErrorEvent.class::isInstance)
                    .map(Events.AutoModeErrorEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals("auto-session-failed", event.sessionId());
            assertEquals("Iteration failed", event.error());
        }
    }

    @Test
    void streamsTypedPreToolHookEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.HookPreToolEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.HookPreToolEvent.class::isInstance)
                    .map(Events.HookPreToolEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals("read_file", event.toolName());
            assertEquals("README.md", event.args().get("path"));
        }
    }

    @Test
    void streamsTypedPostToolHookEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.HookPostToolEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.HookPostToolEvent.class::isInstance)
                    .map(Events.HookPostToolEvent.class::cast)
                    .findFirst().orElseThrow();

            assertTrue(event.success());
            assertEquals(18L, event.duration());
            assertEquals("contents", event.output());
        }
    }

    @Test
    void streamsTypedPrePromptHookEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.HookPrePromptEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.HookPrePromptEvent.class::isInstance)
                    .map(Events.HookPrePromptEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals("Review the SDK", event.instruction());
            assertEquals(List.of("README.md", "pom.xml"), event.mentionedFiles());
        }
    }

    @Test
    void streamsTypedPostResponseHookEventsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.HookPostResponseEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.HookPostResponseEvent.class::isInstance)
                    .map(Events.HookPostResponseEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals(Events.TokenUsageStatus.ACTUAL, event.tokensUsageStatus());
            assertEquals(2, event.toolCallsCount());
            assertEquals(250L, event.duration());
        }
    }

    @Test
    void streamsTypedMcpInvocationRequestsFromSpawnedCli() throws Exception {
        try (AutohandSDK sdk = startedSdk()) {
            Events.McpInvocationRequestEvent event = streamFeatureEvents(sdk).stream()
                    .filter(Events.McpInvocationRequestEvent.class::isInstance)
                    .map(Events.McpInvocationRequestEvent.class::cast)
                    .findFirst().orElseThrow();

            assertEquals("mcp-invoke-1", event.requestId());
            assertEquals("sdk", event.args().get("query"));
        }
    }

    private AutohandSDK startedSdk() throws Exception {
        AutohandSDK sdk = new AutohandSDK(SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .timeoutMs(10_000)
                .build());
        sdk.start();
        return sdk;
    }

    private static List<Event> streamFeatureEvents(AutohandSDK sdk) {
        List<Event> events = new ArrayList<>();
        sdk.streamPrompt(new PromptParams("feature-events"), events::add);
        return events;
    }

    private Path fakeCli() throws Exception {
        Path script = tempDir.resolve("autohand-control-fake-cli");
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
