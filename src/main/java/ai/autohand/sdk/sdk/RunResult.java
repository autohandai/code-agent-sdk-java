package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.AgentStep;
import java.util.List;

public record RunResult(String id, String status, String text, List<Event> events, List<AgentStep> steps) {
    public RunResult(String id, String status, String text, List<Event> events) {
        this(id, status, text, events, List.of());
    }

    public RunResult {
        events = List.copyOf(events);
        steps = List.copyOf(steps);
    }
}
