package ai.autohand.sdk;

import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.PermissionAcknowledgement;
import ai.autohand.sdk.types.DirectoryAccessResponse;
import ai.autohand.sdk.types.DirectoryAccessAcknowledgement;
import ai.autohand.sdk.types.ChangesDecision;
import ai.autohand.sdk.types.SessionHistory;
import ai.autohand.sdk.types.SessionDetails;
import ai.autohand.sdk.types.SDKConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    private AutohandSDK startedSdk() throws Exception {
        AutohandSDK sdk = new AutohandSDK(SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fakeCli().toString())
                .timeoutMs(10_000)
                .build());
        sdk.start();
        return sdk;
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
