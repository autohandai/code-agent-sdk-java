package ai.autohand.sdk;

import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.JsonRpcNotification;
import ai.autohand.sdk.types.SDKConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

class WireNotificationTest {
    @TempDir Path directory;

    @Test void ephemeralIsExplicit() {
        assertFalse(SDKConfig.builder().build().cliArgs().contains("--ephemeral"));
        var config=SDKConfig.builder().ephemeral(true).build();
        assertTrue(config.cliArgs().contains("--ephemeral"));
        assertTrue(config.toBuilder().build().cliArgs().contains("--ephemeral"));
    }

    @Test void rejectsObsoleteOptionsBeforeLaunching() {
        var config=SDKConfig.builder().cliPath("/does-not-exist").autoSaveInterval(30).build();
        var error=assertThrows(IllegalArgumentException.class,config::cliArgs);
        assertTrue(error.getMessage().contains("unsupported by the current CLI"));
    }

    @Test void preservesPacketsAndIndependentObserversAcrossClientRebuild() throws Exception {
        var cli = directory.resolve("cli.py");
        Files.writeString(cli, """
            #!/usr/bin/env python3
            import sys,json
            for line in sys.stdin:
                request=json.loads(line)
                for method,params in [('autohand.pipe.output',{'type':'progress','future':False}),('autohand.future',[1,None,False]),('autohand.future',None)]:
                    print(json.dumps({'jsonrpc':'2.0','method':method,'params':params}),flush=True)
                print(json.dumps({'jsonrpc':'2.0','id':request['id'],'result':{'status':'idle','sessionId':'fixture','model':'fixture','workspace':'fixture','contextPercent':0,'messageCount':0}}),flush=True)
            """);
        assertTrue(cli.toFile().setExecutable(true));
        var packets = new CopyOnWriteArrayList<JsonRpcNotification>();
        try (var sdk = new AutohandSDK(SDKConfig.builder().cwd(directory.toString()).cliPath(cli.toString()).build())) {
            sdk.onNotification(packet -> { throw new IllegalStateException("observer failure"); });
            sdk.onNotification(packet -> { if (packet.params() instanceof ArrayNode array) array.add("mutated"); });
            var remove = sdk.onNotification(packets::add);
            sdk.start(); // Rebuilds the RPC client after observer registration.
            sdk.getState();
            assertEquals(3, packets.size());
            assertEquals("autohand.pipe.output", packets.get(0).method());
            assertEquals("progress", packets.get(0).params().path("type").asText());
            assertEquals(new ObjectMapper().readTree("[1,null,false]"), packets.get(1).params());
            assertTrue(packets.get(2).params().isNull());
            remove.run();
            sdk.getState();
            assertEquals(3, packets.size());
        }
    }
}
