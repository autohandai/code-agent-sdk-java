package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * AUTOHAND_ prefixed environment variables supported by CLI-3.
 */
public record AutohandEnvVars(
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_HOME,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_API_URL,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_CONFIG,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_DEBUG,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_CLIENT_NAME,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_CLIENT_VERSION,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_CODE,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_LOCALE,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_NO_BANNER,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_NON_INTERACTIVE,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_PERMISSION_CALLBACK_TIMEOUT,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_PERMISSION_CALLBACK_URL,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_SECRET,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_SHARE_URL,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_SKIP_PING,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_SKIP_UPDATE_CHECK,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_STREAM_TOOL_OUTPUT,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_TERMINAL_REGIONS,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_THINKING_LEVEL,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_TMUX_LAUNCHED,
    @JsonInclude(JsonInclude.Include.NON_NULL) String AUTOHAND_YES
) {
}
