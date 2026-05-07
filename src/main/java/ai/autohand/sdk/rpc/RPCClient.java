package ai.autohand.sdk.rpc;

import ai.autohand.sdk.transport.Transport;
import ai.autohand.sdk.types.Events;
import ai.autohand.sdk.types.Event;
import ai.autohand.sdk.types.PromptParams;
import java.time.Instant;
import java.util.function.Consumer;

/** Minimal RPC client scaffold used by the high-level SDK. */
public final class RPCClient {
    private final Transport transport;

    public RPCClient(Transport transport) {
        this.transport = transport;
    }

    public void prompt(PromptParams params, Consumer<Event> onEvent) {
        onEvent.accept(new Events.MessageUpdateEvent("msg-1", "", Instant.now().toString()));
        onEvent.accept(new Events.AgentEndEvent("session", "completed", Instant.now().toString()));
    }

    public Transport transport() {
        return transport;
    }
}
