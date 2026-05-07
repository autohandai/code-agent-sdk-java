package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Hooks configuration settings.
 */
public record HooksSettings(
    @JsonInclude(JsonInclude.Include.NON_NULL) Boolean enabled,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<HookDefinition> hooks
) {
}
