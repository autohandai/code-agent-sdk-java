package ai.autohand.sdk;

import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.GetStateResult;
import ai.autohand.sdk.types.SDKConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StartupBenchmarkTest {
    private static final int WARMUPS = 5;
    private static final int SAMPLES = 50;
    private static final double LIMIT_MS = 50.0;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void publicPackageLoadAndUsableCliStartupStayBelowFiftyMillisecondsAtP95() throws Exception {
        Path fixtureSource = tempDir.resolve("fake_rpc_cli.c");
        Path fixture = tempDir.resolve("fake-rpc-cli");
        try (var source = StartupBenchmarkTest.class.getResourceAsStream("/fake_rpc_cli.c")) {
            if (source == null) {
                throw new IllegalStateException("Missing fake_rpc_cli.c benchmark fixture.");
            }
            Files.copy(source, fixtureSource);
        }
        Process compile = new ProcessBuilder("cc", "-O2", fixtureSource.toString(), "-o", fixture.toString())
                .redirectErrorStream(true)
                .start();
        String compilerOutput = new String(compile.getInputStream().readAllBytes());
        assertEquals(0, compile.waitFor(), compilerOutput);

        List<Double> publicImport = samplePublicImport();
        StartupSamples startup = sampleStartup(fixture);
        Map<String, MetricResult> metrics = new LinkedHashMap<>();
        metrics.put("publicImportMs", MetricResult.of(publicImport));
        metrics.put("sdkStartReturnMs", MetricResult.of(startup.startReturnMs()));
        metrics.put("fixtureSpawnToFirstRpcMs", MetricResult.of(startup.firstRpcMs()));
        BenchmarkResult result = new BenchmarkResult(
                "java", LIMIT_MS, metrics, metrics.values().stream().allMatch(MetricResult::passed));

        System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result));

        assertTrue(metrics.get("publicImportMs").passed(),
                () -> "Fresh-process public import p95 was " + metrics.get("publicImportMs").p95Ms() + " ms");
        assertTrue(metrics.get("sdkStartReturnMs").passed(),
                () -> "SDK start-return p95 was " + metrics.get("sdkStartReturnMs").p95Ms() + " ms");
        assertTrue(metrics.get("fixtureSpawnToFirstRpcMs").passed(),
                () -> "Fixture spawn-through-successful-getState p95 was "
                        + metrics.get("fixtureSpawnToFirstRpcMs").p95Ms() + " ms");
    }

    private List<Double> samplePublicImport() throws Exception {
        List<Double> samples = new ArrayList<>();
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");
        for (int index = 0; index < WARMUPS + SAMPLES; index++) {
            Process child = new ProcessBuilder(java, "-cp", classpath, PackageLoadProbe.class.getName())
                    .redirectErrorStream(true)
                    .start();
            String output = new String(child.getInputStream().readAllBytes()).trim();
            assertEquals(0, child.waitFor(), output);
            if (index >= WARMUPS) {
                samples.add(Long.parseLong(output) / 1_000_000.0);
            }
        }
        return samples;
    }

    private StartupSamples sampleStartup(Path fixture) throws Exception {
        List<Double> startReturn = new ArrayList<>();
        List<Double> firstRpc = new ArrayList<>();
        SDKConfig config = SDKConfig.builder()
                .cwd(tempDir.toString())
                .cliPath(fixture.toString())
                .timeoutMs(5_000)
                .build();
        for (int index = 0; index < WARMUPS + SAMPLES; index++) {
            long started = System.nanoTime();
            try (AutohandSDK sdk = new AutohandSDK(config)) {
                sdk.start();
                long startedReturned = System.nanoTime();
                GetStateResult state = sdk.getState();
                if (index >= WARMUPS) {
                    startReturn.add((startedReturned - started) / 1_000_000.0);
                    firstRpc.add((System.nanoTime() - started) / 1_000_000.0);
                }
                assertEquals("idle", state.status());
            }
        }
        return new StartupSamples(startReturn, firstRpc);
    }

    private record StartupSamples(List<Double> startReturnMs, List<Double> firstRpcMs) {
    }

    private record BenchmarkResult(
            String language,
            double budgetMs,
            Map<String, MetricResult> metrics,
            boolean passed) {
    }

    private record MetricResult(int samples, double medianMs, double p95Ms, double maxMs, boolean passed) {
        private static MetricResult of(List<Double> values) {
            List<Double> sorted = new ArrayList<>(values);
            Collections.sort(sorted);
            double median = round(percentile(sorted, 0.50));
            double p95 = round(percentile(sorted, 0.95));
            double max = round(sorted.getLast());
            return new MetricResult(sorted.size(), median, p95, max, p95 < LIMIT_MS);
        }

        private static double percentile(List<Double> sorted, double percentile) {
            int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
            return sorted.get(index);
        }

        private static double round(double value) {
            return Math.round(value * 1_000.0) / 1_000.0;
        }
    }
}
