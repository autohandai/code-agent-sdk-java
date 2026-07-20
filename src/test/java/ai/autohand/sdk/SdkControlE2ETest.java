package ai.autohand.sdk;

import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.PermissionAcknowledgement;
import ai.autohand.sdk.types.DirectoryAccessResponse;
import ai.autohand.sdk.types.DirectoryAccessAcknowledgement;
import ai.autohand.sdk.types.SDKConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
