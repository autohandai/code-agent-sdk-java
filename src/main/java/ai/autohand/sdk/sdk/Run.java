package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.PromptParams;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class Run {
    private final String id = "run-" + UUID.randomUUID();
    private final AutohandSDK sdk;
    private final String prompt;
    private final List<Event> events = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();
    private boolean completed;

    Run(AutohandSDK sdk, String prompt) {
        this.sdk = sdk;
        this.prompt = prompt;
    }

    public void stream(Consumer<Event> onEvent) {
        if (completed) {
            events.forEach(onEvent);
            return;
        }

        sdk.streamPrompt(new PromptParams(prompt), event -> {
            record(event);
            onEvent.accept(event);
        });
        completed = true;
    }

    public RunResult waitForResult() {
        if (!completed) {
            stream(event -> {
            });
        }
        return new RunResult(id, "completed", text.toString(), List.copyOf(events));
    }

    public <T> T json(Class<T> type) throws StructuredOutputError {
        RunResult result = waitForResult();
        if (type == String.class) {
            return type.cast(result.text());
        }
        return JsonParser.parseJsonText(result.text(), type);
    }

    String prompt() {
        return prompt;
    }

    private void record(Event event) {
        events.add(event);
        if (event instanceof Events.MessageUpdateEvent update && update.delta() != null) {
            text.append(update.delta());
        } else if (event instanceof Events.MessageEndEvent end && end.content() != null && text.isEmpty()) {
            text.append(end.content());
        }
    }
}
