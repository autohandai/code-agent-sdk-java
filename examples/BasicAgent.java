import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

/**
 * Basic Agent - Comprehensive configuration example.
 *
 * Demonstrates loading config from file, workspace config, inline configuration,
 * provider detection, permissions, skills, context, session, and AGENTS.md.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="BasicAgent"
 */
public class BasicAgent {
    public static void main(String[] args) throws Exception {
        // Option 1: Load config from file
        // SDKConfig config = loadConfigFrom("~/.autohand/config.json");
        // AutohandSDK sdk = new AutohandSDK(config);

        // Option 2: Load workspace config (merges with global config if available)
        // SDKConfig config = loadWorkspaceConfig();
        // AutohandSDK sdk = new AutohandSDK(config);

        // Option 3: Use inline configuration with new features
        String model = "z-ai/glm-4.5-air:free";
        String provider = detectProviderFromModel(model);
        System.out.println("Detected provider: " + provider);

        // Configure permissions (CLI-3 compatible)
        PermissionSettings permissions = new PermissionSettings(
            "interactive",
            java.util.List.of(new PermissionRule("read_file", true), new PermissionRule("write_file", true), new PermissionRule("git_status", true)),
            java.util.List.of(new PermissionRule("delete_path", false), new PermissionRule("run_command", false)),
            java.util.List.of("git *", "npm install"),
            java.util.List.of("rm -rf", "sudo")
        );

        // Configure context management
        ContextSettings context = new ContextSettings(true, 128_000, 0.7, 0.9, true);

        // Configure session management
        SessionSettings session = new SessionSettings(true, false, "./.autohand/sessions", 60);

        // Configure AGENTS.md
        AgentsMdSettings agentsMd = new AgentsMdSettings(true, "./AGENTS.md", true, true, true, true, true);

        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            ".", cliPath, false, 300_000,
            model, null, null, null, 0.7,
            provider, System.getenv("OPENROUTER_API_KEY"), null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            "interactive", permissions, null, null, null,
            true, true, false, null, null, null,
            java.util.List.of(
                new SkillReference("typescript", null, null),
                new SkillReference("react", null, null),
                new SkillReference("git", null, null)
            ), true,
            context, true,
            null, null, null, null,
            session, true, null, false, false,
            null, null,
            null, null,
            null, null,
            null,
            null,
            agentsMd,
            null, null, null, null, null
        ));

        try {
            sdk.start();
            System.out.println("SDK started");

            // Get session metadata
            SessionMetadata metadata = sdk.getSessionMetadata();
            System.out.println("Session ID: " + metadata.sessionId());
            System.out.println("Project: " + metadata.projectName());
            System.out.println("Model: " + metadata.model());

            // Get session stats
            SessionStats stats = sdk.getStats();
            System.out.println("Initial stats: " + stats);

            // Demonstrate AGENTS.md loading
            try {
                String agentsContent = loadAgentsMdFile("./AGENTS.md");
                System.out.println("Loaded AGENTS.md");
                System.out.println("Content preview: " + agentsContent.substring(0, Math.min(200, agentsContent.length())) + "...");
            } catch (Exception e) {
                String template = AutohandSDK.createDefaultAgentsMd("My Project");
                System.out.println("Created default AGENTS.md template");
                System.out.println("Template: " + template.substring(0, Math.min(200, template.length())) + "...");
            }

            // Stream prompt
            sdk.streamPrompt(new PromptParams("Hello, what can you help me with today?"), event -> {
                switch (event) {
                    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
                    case Events.ToolStartEvent e -> System.out.println("\n[Tool: " + e.toolName() + "]");
                    default -> {}
                }
            });
            System.out.println();

            // Get updated stats after the prompt
            SessionStats finalStats = sdk.getStats();
            System.out.println("Final stats: " + finalStats);

            // Save session manually
            sdk.saveSession();
            System.out.println("Session saved");

            sdk.stop();
            System.out.println("SDK stopped");

            // Demonstrate session resumption
            System.out.println("\n--- Session Resumption Demo ---");
            AutohandSDK resumedSdk = new AutohandSDK(new SDKConfig(
                ".", cliPath, false, 300_000,
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
            resumedSdk.resumeSession(metadata.sessionId());
            System.out.println("Resumed session: " + metadata.sessionId());

            // Continue the conversation
            resumedSdk.streamPrompt(new PromptParams("What was my previous message?"), event -> {
                if (event instanceof Events.MessageUpdateEvent e) {
                    System.out.print(e.delta());
                }
            });
            System.out.println();

            resumedSdk.stop();
            System.out.println("Resumed session stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    static String detectProviderFromModel(String model) {
        if (model == null) return null;
        if (model.startsWith("z-ai/")) return "zai";
        if (model.startsWith("openai/")) return "openai";
        if (model.startsWith("anthropic/")) return "anthropic";
        if (model.startsWith("google/")) return "google";
        return null;
    }

    static String loadAgentsMdFile(String path) throws Exception {
        java.nio.file.Path p = java.nio.file.Paths.get(path);
        if (!java.nio.file.Files.exists(p)) throw new java.io.FileNotFoundException(path);
        return java.nio.file.Files.readString(p);
    }
}
