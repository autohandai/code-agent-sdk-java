package ai.autohand.sdk.types;

import java.util.List;
import java.util.concurrent.CompletionStage;

/** Host-only asynchronous decision evaluated after persisted tool results. */
@FunctionalInterface
public interface StopCondition {
    CompletionStage<Boolean> shouldStop(Context context);

    /** Immutable history of completed steps in the current prompt. */
    record Context(List<AgentStep> steps) {
        public Context { steps = List.copyOf(steps); }
    }
}
