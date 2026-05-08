package ai.autohand.sdk.sdk;

/** JSON-RPC error returned by the Autohand CLI. */
public final class RpcException extends AutohandException {
    private static final long serialVersionUID = 1L;

    private final String method;
    private final int code;

    public RpcException(String method, int code, String message) {
        super("Autohand RPC request failed: " + method + " (" + code + "): " + message);
        this.method = method;
        this.code = code;
    }

    public String method() {
        return method;
    }

    public int code() {
        return code;
    }
}
