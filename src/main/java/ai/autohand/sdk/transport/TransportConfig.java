package ai.autohand.sdk.transport;

import java.util.List;
import java.util.Map;

public record TransportConfig(
        String cwd,
        String cliPath,
        boolean debug,
        int timeoutMs,
        List<String> args,
        Map<String, String> environment
) {
    public TransportConfig {
        timeoutMs = timeoutMs <= 0 ? 300_000 : timeoutMs;
        args = args == null ? List.of() : List.copyOf(args);
        environment = environment == null ? Map.of() : Map.copyOf(environment);
    }

    public TransportConfig(String cwd, String cliPath, boolean debug, int timeoutMs) {
        this(cwd, cliPath, debug, timeoutMs, List.of("--mode", "rpc"), Map.of());
    }
}
