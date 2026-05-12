package ai.autohand.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExampleCompileTest {
    @TempDir
    Path outputDir;

    @Test
    void allExamplesCompileAgainstPublishedApi() throws Exception {
        List<String> examples;
        try (Stream<Path> stream = Files.list(Path.of("examples"))) {
            examples = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .map(Path::toString)
                    .toList();
        }

        assertEquals(29, examples.size(), "Keep this aligned with the TypeScript example set.");

        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Run tests with a JDK, not a JRE.");

        List<String> args = new java.util.ArrayList<>();
        args.add("--release");
        args.add("21");
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        args.add("-d");
        args.add(outputDir.toString());
        args.addAll(examples);

        int exitCode = compiler.run(null, System.out, System.err, args.toArray(String[]::new));
        assertEquals(0, exitCode);
    }
}
