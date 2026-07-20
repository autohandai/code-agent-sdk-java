package ai.autohand.sdk.types;

/** Marker interface for SDK stream events. */
public sealed interface Event permits Events.AgentStartEvent, Events.AgentEndEvent,
        Events.TurnStartEvent, Events.TurnEndEvent, Events.MessageStartEvent,
        Events.MessageUpdateEvent, Events.MessageEndEvent, Events.ToolStartEvent,
        Events.ToolUpdateEvent, Events.ToolEndEvent, Events.PermissionRequestEvent,
        Events.FileModifiedEvent, Events.AutoresearchLifecycleEvent,
        Events.AutoresearchOperationEvent, Events.AutoModeIterationEvent,
        Events.AutoModeCompleteEvent,
        Events.AutoModeErrorEvent,
        Events.HookPreToolEvent,
        Events.HookPostToolEvent,
        Events.HookPrePromptEvent,
        Events.ErrorEvent, Events.UnknownEvent {
}
