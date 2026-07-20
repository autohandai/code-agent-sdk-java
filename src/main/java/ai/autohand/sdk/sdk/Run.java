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
    private State state = State.NEW;
    private RuntimeException failure;

    private enum State {
        NEW,
        RUNNING,
        COMPLETED,
        FAILED
    }

    Run(AutohandSDK sdk, String prompt) {
        this.sdk = sdk;
        this.prompt = prompt;
    }

    public void stream(Consumer<Event> onEvent) {
        synchronized (this) {
            if (state == State.COMPLETED) {
                List.copyOf(events).forEach(onEvent);
                return;
            }
            if (state == State.FAILED) {
                throw failure;
            }
            if (state == State.RUNNING) {
                throw new IllegalStateException("This run is already streaming.");
            }
            state = State.RUNNING;
        }

        try {
            sdk.streamPrompt(new PromptParams(prompt), event -> {
                record(event);
                onEvent.accept(event);
            });
            synchronized (this) {
                state = State.COMPLETED;
                notifyAll();
            }
        } catch (RuntimeException exception) {
            synchronized (this) {
                failure = exception;
                state = State.FAILED;
                notifyAll();
            }
            throw exception;
        }
    }

    public RunResult waitForResult() {
        boolean shouldStart;
        synchronized (this) {
            shouldStart = state == State.NEW;
        }
        if (shouldStart) {
            stream(event -> {
            });
        }
        synchronized (this) {
            while (state == State.RUNNING) {
                try {
                    wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AutohandException("Interrupted while waiting for the run to complete.", exception);
                }
            }
            if (state == State.FAILED) {
                throw failure;
            }
            return new RunResult(id, "completed", text.toString(), List.copyOf(events));
        }
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

    private synchronized void record(Event event) {
        events.add(event);
        if (event instanceof Events.MessageUpdateEvent update && update.delta() != null) {
            text.append(update.delta());
        } else if (event instanceof Events.MessageEndEvent end && end.content() != null && text.isEmpty()) {
            text.append(end.content());
        }
    }
}
