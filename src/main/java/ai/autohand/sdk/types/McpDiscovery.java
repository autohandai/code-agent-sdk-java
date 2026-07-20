package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Map;

/** Typed MCP discovery payloads exposed by the Autohand CLI. */
public final class McpDiscovery {
    private McpDiscovery() {
    }

    public record Server(String name, String status, int toolCount) {
    }

    public record ListServersResult(List<Server> servers) {
    }

    public record ListToolsParams(@JsonInclude(JsonInclude.Include.NON_NULL) String serverName) {
        public static ListToolsParams allServers() {
            return new ListToolsParams(null);
        }
    }

    public record Tool(String name, String description, String serverName) {
    }

    public record ListToolsResult(List<Tool> tools) {
    }

    public enum Transport {
        STDIO("stdio"),
        SSE("sse"),
        HTTP("http");

        private final String cliValue;

        Transport(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }
    }

    public record ServerConfig(
            String name,
            Transport transport,
            String command,
            List<String> args,
            String url,
            Map<String, String> env,
            Map<String, String> headers,
            Boolean autoConnect) {
    }

    public record GetServerConfigsResult(List<ServerConfig> configs) {
    }
}
