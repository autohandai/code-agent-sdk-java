package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

public record HookDefinition(
        String event,
        String command,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean enabled,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer timeoutSeconds,
        @JsonInclude(JsonInclude.Include.NON_NULL) String matcher,
        @JsonInclude(JsonInclude.Include.NON_NULL) HookFilter filter
) {
    public HookDefinition(String event, String command, String description, boolean enabled, int timeoutSeconds) {
        this(event, command, description, enabled, timeoutSeconds, null, null);
    }

    public HookDefinition(HookEvent event, String command, String description, boolean enabled, int timeoutSeconds) {
        this(event.toCliString(), command, description, enabled, timeoutSeconds, null, null);
    }

    public HookDefinition(HookEvent event, String command, boolean enabled, HookFilter filter, int timeoutSeconds) {
        this(event.toCliString(), command, null, enabled, timeoutSeconds, null, filter);
    }
}
