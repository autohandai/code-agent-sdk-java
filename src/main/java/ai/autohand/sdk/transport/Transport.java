package ai.autohand.sdk.transport;

import java.io.IOException;

/** Minimal subprocess transport placeholder for the CLI JSON-RPC layer. */
public final class Transport implements AutoCloseable {
    private final TransportConfig config;
    private boolean running;

    public Transport(TransportConfig config) {
        this.config = config;
    }

    public void start() throws IOException {
        running = true;
    }

    public boolean isRunning() {
        return running;
    }

    public TransportConfig config() {
        return config;
    }

    @Override
    public void close() {
        running = false;
    }
}
