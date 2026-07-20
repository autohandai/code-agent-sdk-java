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
    private final BlockingQueue<OutputItem> stdoutLines = new LinkedBlockingQueue<>();
    private final Queue<String> stderrTail = new ArrayDeque<>();
    private final Object writeLock = new Object();
    private volatile Process process;
    private volatile BufferedWriter stdin;
    private volatile boolean running;
    private volatile long generation;

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

        stdoutLines.clear();
        synchronized (stderrTail) {
            stderrTail.clear();
        }
        Process cliProcess = builder.start();
        long processGeneration = generation + 1;
        generation = processGeneration;
        process = cliProcess;
        stdin = new BufferedWriter(new OutputStreamWriter(cliProcess.getOutputStream(), StandardCharsets.UTF_8));
        running = true;

        startReader("autohand-rpc-stdout", () -> readStdout(cliProcess, processGeneration));
        startReader("autohand-rpc-stderr", () -> readStderr(cliProcess, processGeneration));
    }

    public void writeLine(String line) {
        synchronized (writeLock) {
            BufferedWriter writer = stdin;
            if (!isRunning() || writer == null) {
                throw new TransportException("Autohand CLI process is not running.");
            }
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                throw new TransportException("Failed to write JSON-RPC request to Autohand CLI.", e);
            }
        }
    }

    public String takeLine(Duration timeout) {
        return takeLine(timeout, generation);
    }

    /** Reads output only from the supplied process generation. */
    public String takeLine(Duration timeout, long expectedGeneration) {
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (true) {
                if (generation != expectedGeneration) {
                    throw new TransportException("Autohand CLI transport generation changed while awaiting output.");
                }

                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0) {
                    return null;
                }
                long waitMs = Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
                OutputItem item = stdoutLines.poll(waitMs, TimeUnit.MILLISECONDS);
                long currentGeneration = generation;
                if (currentGeneration != expectedGeneration) {
                    if (item != null && item.generation() == currentGeneration) {
                        stdoutLines.offer(item);
                    }
                    throw new TransportException("Autohand CLI transport generation changed while awaiting output.");
                }
                if (item != null && item.generation() != expectedGeneration) {
                    continue;
                }
                if (item != null && item.closed()) {
                    throw new TransportException("Autohand CLI transport closed before returning a JSON-RPC response."
                            + stderrSuffix());
                }
                if (item != null) {
                    return item.line();
                }
                if (!running || process == null || !process.isAlive()) {
                    throw new TransportException("Autohand CLI exited before returning a JSON-RPC response."
                            + stderrSuffix());
                }
                return null;
            }
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

    /** Returns the currently active process generation. */
    public long generation() {
        return generation;
    }

    @Override
    public synchronized void close() {
        long closingGeneration = generation;
        running = false;
        stdoutLines.offer(OutputItem.closed(closingGeneration));

        synchronized (writeLock) {
            BufferedWriter writer = stdin;
            stdin = null;
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                }
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

    private void readStdout(Process cliProcess, long processGeneration) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(cliProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (generation != processGeneration || process != cliProcess) {
                    break;
                }
                stdoutLines.offer(OutputItem.line(processGeneration, line));
            }
        } catch (IOException e) {
            if (running && generation == processGeneration && process == cliProcess && config.debug()) {
                System.err.println("[autohand-sdk] stdout reader failed: " + e.getMessage());
            }
        } finally {
            if (generation == processGeneration && process == cliProcess) {
                running = false;
            }
            stdoutLines.offer(OutputItem.closed(processGeneration));
        }
    }

    private void readStderr(Process cliProcess, long processGeneration) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(cliProcess.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (generation != processGeneration || process != cliProcess) {
                    break;
                }
                rememberStderr(line);
                if (config.debug()) {
                    System.err.println("[autohand-cli] " + line);
                }
            }
        } catch (IOException e) {
            if (running && generation == processGeneration && process == cliProcess && config.debug()) {
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

    private record OutputItem(long generation, String line, boolean closed) {
        private static OutputItem line(long generation, String line) {
            return new OutputItem(generation, line, false);
        }

        private static OutputItem closed(long generation) {
            return new OutputItem(generation, null, true);
        }
    }
}
