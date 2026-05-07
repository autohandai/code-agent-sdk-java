package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Information about an available LLM model.
 *
 * @param id the unique model identifier
 * @param displayName the human-readable model name
 * @param description optional model description
 * @see AutohandSDK#supportedModels()
 */
public record ModelInfo(
    String id,
    String displayName,
    @JsonInclude(JsonInclude.Include.NON_NULL) String description
) {
}
