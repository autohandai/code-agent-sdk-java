package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record PromptParams(String message, @JsonIgnore List<StopCondition> stopWhen) {
    public PromptParams(String message) { this(message, List.of()); }

    public PromptParams {
        stopWhen = List.copyOf(stopWhen);
    }

    public PromptParams withStopWhen(StopCondition... conditions) {
        return new PromptParams(message, List.of(conditions));
    }
}
