package ai.autohand.sdk;
import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.SDKConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
class CurrentCliParityTest {
 @TempDir Path directory;
 @Test void autoCommitUsesLongFlag() {
  var args = SDKConfig.builder().autoCommit(true).build().cliArgs();
  org.junit.jupiter.api.Assertions.assertTrue(args.contains("--auto-commit"));
  org.junit.jupiter.api.Assertions.assertFalse(args.contains("-c"));
 }

 @Test void browserCapabilitiesUseExactContract() throws Exception {
  Path cli=directory.resolve("autohand");
  Files.copy(Path.of("tests/fixtures/current-cli-controls.py"),cli);
  if (!cli.toFile().setExecutable(true)) throw new IllegalStateException("Unable to mark fixture executable");
  try(var sdk=new AutohandSDK(SDKConfig.builder().cliPath(cli.toString()).cwd(directory.toString()).timeoutMs(10000).build())) {
   sdk.start();
   var result=sdk.setBrowserCapabilities(2,"1.0.0",List.of("browser_navigate"));
   assertEquals("autohand.browserCapabilities.set",result.path("method").asText());
   assertEquals(2,result.path("params").path("protocolVersion").asInt());
   assertEquals("1.0.0",result.path("params").path("extensionVersion").asText());
   assertEquals("browser_navigate",result.path("params").path("tools").get(0).asText());
  }
 }
}
