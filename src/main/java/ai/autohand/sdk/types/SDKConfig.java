package ai.autohand.sdk.types;

import java.util.Arrays;

/** Configuration for the CLI-backed SDK. The long varargs tail preserves compatibility with generated examples. */
public final class SDKConfig {
    private final String cwd;
    private final String cliPath;
    private final boolean debug;
    private final int timeoutMs;
    private final Object[] options;

    public SDKConfig(String cwd, String cliPath, boolean debug, int timeoutMs, Object... options) {
        this.cwd = cwd;
        this.cliPath = cliPath;
        this.debug = debug;
        this.timeoutMs = timeoutMs;
        this.options = options == null ? new Object[0] : Arrays.copyOf(options, options.length);
    }

    public String cwd() {
        return cwd;
    }

    public String cliPath() {
        return cliPath;
    }

    public boolean debug() {
        return debug;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public Object[] options() {
        return Arrays.copyOf(options, options.length);
    }

    public String model() {
        return options.length > 0 && options[0] instanceof String value ? value : null;
    }
}
