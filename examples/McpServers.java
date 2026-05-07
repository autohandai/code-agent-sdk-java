import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * MCP Servers Example - Configure and use MCP (Model Context Protocol) servers.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="McpServers"
 */
public class McpServers {
    public static void main(String[] args) throws Exception {
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"), cliPath, false, 300_000,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        ));

        // Example MCP server configuration
        // sdk.setMcpServers(Map.of(
        //     "filesystem", new McpServerConfig("stdio", "npx", List.of("-y", "@modelcontextprotocol/server-filesystem", "/Users/user/projects")),
        //     "fetch", new McpServerConfig("stdio", "uvx", List.of("mcp-server-fetch"))
        // ));

        try {
            sdk.start();
            System.out.println("SDK started with MCP server support");

            System.out.println("\nAvailable models:");
            for (ModelInfo model : sdk.supportedModels()) {
                System.out.println("  - " + model.id() + " (" + model.provider() + ")");
            }

            sdk.streamPrompt(new PromptParams("List any available MCP tools you can use."), event -> {
                switch (event) {
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.ToolStartEvent e -> System.out.println("\n[MCP Tool: " + e.toolName() + "]");
                    case Events.ToolEndEvent e -> System.out.println("[MCP Tool completed: " + e.toolName() + "]");
                    default -> {}
                }
            });

            sdk.stop();
            System.out.println("\nSDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            sdk.stop();
            System.exit(1);
        }
    }
}
