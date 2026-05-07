package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Filter to limit when a hook fires.
 */
public record HookFilter(
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> tool,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> path
) {
}
