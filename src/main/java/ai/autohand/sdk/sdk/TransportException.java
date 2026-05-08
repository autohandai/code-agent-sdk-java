package ai.autohand.sdk.sdk;

/** Failure while starting, stopping, or communicating with the CLI subprocess. */
public final class TransportException extends AutohandException {
    private static final long serialVersionUID = 1L;

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
