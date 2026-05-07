package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Result types for hook operations.
 */
public final class HookResultTypes {
    private HookResultTypes() {}

    public record GetHooksResult(HooksSettings settings) {}

    public record AddHookResult(boolean success, @JsonInclude(JsonInclude.Include.NON_NULL) String hookId) {}

    public record RemoveHookResult(boolean success) {}

    public record ToggleHookResult(boolean success, boolean enabled) {}

    public record HookResponse(
        @JsonInclude(JsonInclude.Include.NON_NULL) String decision,
        @JsonInclude(JsonInclude.Include.NON_NULL) String reason,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean continue_,
        @JsonInclude(JsonInclude.Include.NON_NULL) String stopReason,
        @JsonInclude(JsonInclude.Include.NON_NULL) java.util.Map<String, Object> updatedInput,
        @JsonInclude(JsonInclude.Include.NON_NULL) String additionalContext
    ) {}

    public record HookExecutionResult(
        HookDefinition hook,
        boolean success,
        @JsonInclude(JsonInclude.Include.NON_NULL) String stdout,
        @JsonInclude(JsonInclude.Include.NON_NULL) String stderr,
        @JsonInclude(JsonInclude.Include.NON_NULL) String error,
        long duration,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer exitCode,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean blockingError,
        @JsonInclude(JsonInclude.Include.NON_NULL) HookResponse response
    ) {}

    public record TestHookResult(
        HookDefinition hook,
        boolean success,
        @JsonInclude(JsonInclude.Include.NON_NULL) String stdout,
        @JsonInclude(JsonInclude.Include.NON_NULL) String stderr,
        @JsonInclude(JsonInclude.Include.NON_NULL) String error,
        long duration,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer exitCode,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean blockingError,
        @JsonInclude(JsonInclude.Include.NON_NULL) HookResponse response,
        boolean testMode
    ) {}
}
