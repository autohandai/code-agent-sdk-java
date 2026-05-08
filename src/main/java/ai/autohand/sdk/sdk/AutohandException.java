package ai.autohand.sdk.sdk;

/** Base unchecked exception for SDK transport, RPC, and lifecycle failures. */
public class AutohandException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AutohandException(String message) {
        super(message);
    }

    public AutohandException(String message, Throwable cause) {
        super(message, cause);
    }
}
