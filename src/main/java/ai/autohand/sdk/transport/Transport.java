package ai.autohand.sdk.transport;

import ai.autohand.sdk.sdk.TransportException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Subprocess transport for the Autohand CLI JSON-RPC mode. */
public final class Transport implements AutoCloseable {
    private static final int STDERR_LINES_TO_KEEP = 40;

    private final TransportConfig config;
    private final BlockingQueue<String> stdoutLines = new LinkedBlockingQueue<>();
    private final Queue<String> stderrTail = new ArrayDeque<>();
    private final Object writeLock = new Object();
    private volatile Process process;
    private volatile BufferedWriter stdin;
    private volatile boolean running;

    public Transport(TransportConfig config) {
        this.config = config;
    }

    public synchronized void start() throws IOException {
        if (isRunning()) {
            return;
        }

        List<String> command = new ArrayList<>();
        command.add(cliPath());
        command.addAll(config.args());

        ProcessBuilder builder = new ProcessBuilder(command);
        if (config.cwd() != null && !config.cwd().isBlank()) {
            builder.directory(new File(config.cwd()));
        }

        Map<String, String> env = builder.environment();
        env.put("AUTOHAND_STREAM_TOOL_OUTPUT", "1");
        env.putAll(config.environment());

        if (config.debug()) {
            System.err.println("[autohand-sdk] starting: " + String.join(" ", command));
            System.err.println("[autohand-sdk] cwd: " + builder.directory());
        }

        process = builder.start();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        running = true;

        startReader("autohand-rpc-stdout", () -> readStdout(process));
        startReader("autohand-rpc-stderr", () -> readStderr(process));
    }

    public void writeLine(String line) {
        if (!isRunning() || stdin == null) {
            throw new TransportException("Autohand CLI process is not running.");
        }

        synchronized (writeLock) {
            try {
                stdin.write(line);
                stdin.newLine();
                stdin.flush();
            } catch (IOException e) {
                throw new TransportException("Failed to write JSON-RPC request to Autohand CLI.", e);
            }
        }
    }

    public String takeLine(Duration timeout) {
        try {
            String line = stdoutLines.poll(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
            if (line == null && process != null && !process.isAlive() && stdoutLines.isEmpty()) {
                throw new TransportException("Autohand CLI exited before returning a JSON-RPC response." + stderrSuffix());
            }
            return line;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportException("Interrupted while waiting for Autohand CLI output.", e);
        }
    }

    public boolean isRunning() {
        Process current = process;
        return running && current != null && current.isAlive();
    }

    public TransportConfig config() {
        return config;
    }

    @Override
    public synchronized void close() {
        running = false;

        BufferedWriter writer = stdin;
        stdin = null;
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }

        Process current = process;
        process = null;
        if (current != null && current.isAlive()) {
            current.destroy();
            try {
                if (!current.waitFor(2, TimeUnit.SECONDS)) {
                    current.destroyForcibly();
                    current.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                current.destroyForcibly();
            }
        }
    }

    private void readStdout(Process cliProcess) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(cliProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stdoutLines.offer(line);
            }
        } catch (IOException e) {
            if (running && config.debug()) {
                System.err.println("[autohand-sdk] stdout reader failed: " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    private void readStderr(Process cliProcess) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(cliProcess.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                rememberStderr(line);
                if (config.debug()) {
                    System.err.println("[autohand-cli] " + line);
                }
            }
        } catch (IOException e) {
            if (running && config.debug()) {
                System.err.println("[autohand-sdk] stderr reader failed: " + e.getMessage());
            }
        }
    }

    private void rememberStderr(String line) {
        synchronized (stderrTail) {
            stderrTail.add(line);
            while (stderrTail.size() > STDERR_LINES_TO_KEEP) {
                stderrTail.poll();
            }
        }
    }

    private String stderrSuffix() {
        synchronized (stderrTail) {
            if (stderrTail.isEmpty()) {
                return "";
            }
            return "\nRecent CLI stderr:\n" + String.join("\n", stderrTail);
        }
    }

    private String cliPath() {
        if (config.cliPath() != null && !config.cliPath().isBlank()) {
            return config.cliPath();
        }
        String envPath = System.getenv("AUTOHAND_CLI_PATH");
        return envPath == null || envPath.isBlank() ? "autohand" : envPath;
    }

    private static void startReader(String name, Runnable task) {
        Thread.ofVirtual().name(name).start(task);
    }
}
