package ai.autohand.sdk.transport;

public record TransportConfig(String cwd, String cliPath, boolean debug, int timeoutMs) {
}
