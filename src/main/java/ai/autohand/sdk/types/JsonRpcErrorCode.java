package ai.autohand.sdk.types;

/**
 * JSON-RPC 2.0 standard and application-specific error codes.
 */
public final class JsonRpcErrorCode {
    private JsonRpcErrorCode() {}

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
    public static final int EXECUTION_ERROR = -32000;
    public static final int PERMISSION_DENIED = -32001;
    public static final int TIMEOUT = -32002;
    public static final int AGENT_BUSY = -32003;
    public static final int ABORTED = -32004;
}
