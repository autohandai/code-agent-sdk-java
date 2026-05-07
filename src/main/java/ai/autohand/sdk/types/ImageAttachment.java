package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Image attachment for multimodal prompts.
 */
public record ImageAttachment(
    String data,
    String mimeType,
    @JsonInclude(JsonInclude.Include.NON_NULL) String filename
) {
}
