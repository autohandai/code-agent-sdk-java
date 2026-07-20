package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Typed contracts for enabling, timing, or disabling unrestricted YOLO mode. */
public final class YoloMode {
    private YoloMode() {
    }

    public record Params(
            String pattern,
            @JsonInclude(JsonInclude.Include.NON_NULL) Integer timeoutSeconds) {
        public Params {
            if (pattern == null) {
                throw new IllegalArgumentException("YOLO pattern must not be null; use an empty string to disable it.");
            }
            if (timeoutSeconds != null && timeoutSeconds < 1) {
                throw new IllegalArgumentException("YOLO timeout must be positive.");
            }
        }
    }

    public record Result(boolean success, Integer expiresIn) {
    }
}
