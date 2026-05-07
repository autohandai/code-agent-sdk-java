import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 10 Multi-Tool Reasoning - Using multiple tools across turns.
 *
 * Run with:
 *   AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="MultiToolReasoning"
 */
public class MultiToolReasoning {
    public static void main(String[] args) throws Exception {
        Path tmpdir = Files.createTempDirectory("multi-tool-example-");

        try {
            // Create a small project with a module and a test
            Files.writeString(tmpdir.resolve("MathUtils.java"), """
                public class MathUtils {
                    public static long fibonacci(int n) {
                        if (n <= 0) return 0;
                        if (n == 1) return 1;
                        long a = 0, b = 1;
                        for (int i = 2; i <= n; i++) {
                            long temp = a + b;
                            a = b;
                            b = temp;
                        }
                        return b;
                    }

                    public static long factorial(int n) {
                        if (n < 0) throw new IllegalArgumentException("negative");
                        long result = 1;
                        for (int i = 2; i <= n; i++) result *= i;
                        return result;
                    }
                }
                """);

            Files.writeString(tmpdir.resolve("MathUtilsTest.java"), """
                public class MathUtilsTest {
                    public static void main(String[] args) {
                        assert MathUtils.fibonacci(0) == 0;
                        assert MathUtils.fibonacci(1) == 1;
                        assert MathUtils.fibonacci(5) == 5;
                        assert MathUtils.fibonacci(10) == 55;
                        System.out.println("Fibonacci tests passed");

                        assert MathUtils.factorial(0) == 1;
                        assert MathUtils.factorial(5) == 120;
                        assert MathUtils.factorial(10) == 3628800;
                        System.out.println("Factorial tests passed");
                    }
                }
                """);

            System.out.println("=== Multi-Tool Reasoning Demo ===\n");
            System.out.println("Created test project in: " + tmpdir + "\n");

            String cliPath = System.getenv("AUTOHAND_CLI_PATH");
            AutohandSDK sdk = new AutohandSDK(new SDKConfig(
                tmpdir.toString(), cliPath, false, 300_000,
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

            sdk.start();
            System.out.println("SDK started\n");

            String prompt = "First, glob for all Java files in this directory. Then read each Java file. Finally, run `java MathUtilsTest.java` and report the test results. Summarize the codebase.";
            System.out.println("Sending prompt: \"" + prompt + "\"\n");

            StringBuilder fullResponse = new StringBuilder();
            sdk.streamPrompt(new PromptParams(prompt), event -> {
                switch (event) {
                    case Events.ToolStartEvent e -> System.out.println("[Tool called: " + e.toolName() + "]");
                    case Events.ToolEndEvent e -> {
                        System.out.println("[Tool completed: " + e.toolName() + "]");
                        if (e.output() != null) {
                            String preview = e.output().length() > 1000
                                ? e.output().substring(0, 1000) + "\n... (truncated)"
                                : e.output();
                            System.out.println("Output:\n" + preview);
                        }
                    }
                    case Events.PermissionRequestEvent e -> {
                        System.out.println("[Permission request: " + e.tool() + "]");
                        System.out.println("  Description: " + e.description());
                        try {
                            sdk.permissionResponse(new PermissionResponseParams(e.requestId(), PermissionDecision.ALLOW_ONCE, true, null, null));
                        } catch (Exception ex) {
                            System.err.println("Failed to approve: " + ex.getMessage());
                        }
                    }
                    case Events.MessageUpdateEvent e -> {
                        System.out.print(e.delta());
                        fullResponse.append(e.delta());
                    }
                    case Events.MessageEndEvent e -> {
                        if (e.content() != null) fullResponse.append(e.content());
                    }
                    default -> {}
                }
            });

            System.out.println("\n=== Agent Response ===");
            System.out.println(fullResponse);

            sdk.stop();
            System.out.println("\nSDK stopped");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            deleteDirectory(tmpdir);
            System.out.println("\nCleaned up test directory: " + tmpdir);
        }
    }

    static void deleteDirectory(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }
}
