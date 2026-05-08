package ai.autohand.sdk.sdk;

/** Thrown when the CLI does not answer a JSON-RPC request before the SDK timeout. */
public final class RequestTimeoutException extends AutohandException {
    private static final long serialVersionUID = 1L;

    private final String method;

    public RequestTimeoutException(String method, long timeoutMs) {
        super("Request timeout after " + timeoutMs + "ms: " + method);
        this.method = method;
    }

    public String method() {
        return method;
    }
}
