package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class Run {
    private final String id = "run-" + UUID.randomUUID();
    private final String prompt;
    private final List<Event> events = new ArrayList<>();
    private String text = "";

    Run(String prompt) {
        this.prompt = prompt;
    }

    public void stream(Consumer<Event> onEvent) {
        Event update = new Events.MessageUpdateEvent("msg-1", "", java.time.Instant.now().toString());
        Event end = new Events.AgentEndEvent("session", "completed", java.time.Instant.now().toString());
        events.add(update);
        events.add(end);
        onEvent.accept(update);
        onEvent.accept(end);
    }

    public RunResult waitForResult() {
        return new RunResult(id, "completed", text, List.copyOf(events));
    }

    public <T> T json(Class<T> type) throws StructuredOutputError {
        if (type == String.class) {
            return type.cast(text);
        }
        throw new StructuredOutputError("JSON parsing requires an application validator in the current scaffold", text);
    }

    String prompt() {
        return prompt;
    }
}
