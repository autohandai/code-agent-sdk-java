package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Information about an available subagent.
 */
public record AgentInfo(
    String id,
    String name,
    String description,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<String> tools
) {
}
