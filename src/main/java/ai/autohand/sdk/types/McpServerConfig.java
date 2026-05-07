package ai.autohand.sdk.types;

import java.util.List;
import java.util.Map;

public record McpServerConfig(
        String command,
        List<String> args,
        String url,
        Map<String, String> env,
        Map<String, String> headers,
        boolean autoConnect
) {
}
