# Event Streaming

`streamPrompt` sends a prompt and calls your event handler as events arrive.

```java
sdk.streamPrompt(new PromptParams("Explain this repository"), event -> {
    switch (event) {
        case Events.MessageUpdateEvent e -> System.out.print(e.delta());
        case Events.ToolStartEvent e -> System.out.println("\n[tool] " + e.toolName());
        case Events.ToolUpdateEvent e -> System.out.print(e.output());
        case Events.ToolEndEvent e -> System.out.println("\n[done] " + e.toolName());
        case Events.PermissionRequestEvent e -> {
            System.out.println("\n[permission] " + e.tool() + ": " + e.description());
            sdk.allowPermission(e.requestId(), DecisionScope.ONCE);
        }
        case Events.ErrorEvent e -> System.err.println("\n[error] " + e.message());
        default -> {}
    }
});
```

Use the high-level `Run` API when you want to stream first and then inspect the
final result:

```java
var run = agent.send("Review this package");
run.stream(event -> {
    if (event instanceof Events.MessageUpdateEvent e) {
        System.out.print(e.delta());
    }
});

RunResult result = run.waitForResult();
System.out.println(result.text());
```

## Typed hook notifications

The CLI hook notification surface maps to these `Event` implementations:

| JSON-RPC method | Java event | Key accessors |
| --- | --- | --- |
| `autohand.hook.preTool` | `Events.HookPreToolEvent` | `toolId()`, `toolName()`, `args()` |
| `autohand.hook.postTool` | `Events.HookPostToolEvent` | `success()`, `duration()`, `output()` |
| `autohand.hook.fileModified` | `Events.FileModifiedEvent` | `filePath()`, `fileChangeType()`, `toolId()` |
| `autohand.hook.prePrompt` | `Events.HookPrePromptEvent` | `instruction()`, `mentionedFiles()` |
| `autohand.hook.postResponse` | `Events.HookPostResponseEvent` | `tokensUsed()`, `tokensUsageStatus()`, `toolCallsCount()`, `duration()` |
| `autohand.hook.sessionError` | `Events.HookSessionErrorEvent` | `error()`, `code()`, `context()` |
| `autohand.hook.stop` | `Events.HookStopEvent` | `tokensUsed()`, `tokensUsageStatus()`, `toolCallsCount()`, `duration()` |
| `autohand.hook.sessionStart` | `Events.HookSessionStartEvent` | `sessionType()` |
| `autohand.hook.sessionEnd` | `Events.HookSessionEndEvent` | `reason()`, `duration()` |
| `autohand.hook.subagentStop` | `Events.HookSubagentStopEvent` | `subagentId()`, `subagentName()`, `success()`, `error()` |
| `autohand.hook.permissionRequest` | `Events.HookPermissionRequestEvent` | `tool()`, `path()`, `command()`, `args()` |
| `autohand.hook.notification` | `Events.HookNotificationEvent` | `notificationType()`, `message()` |
| `autohand.hook.contextCompacted` | `Events.HookContextCompactedEvent` | `croppedCount()`, `summary()`, `usagePercent()`, `reason()` |
| `autohand.hook.contextOverflow` | `Events.HookContextOverflowEvent` | `tokensBefore()`, `tokensAfter()`, `croppedCount()`, `usagePercent()` |
| `autohand.hook.contextWarning` | `Events.HookContextWarningEvent` | `usagePercent()`, `remainingTokens()` |
| `autohand.hook.contextCritical` | `Events.HookContextCriticalEvent` | `usagePercent()`, `remainingTokens()` |

Every typed hook also exposes `timestamp()`. Token usage status is either
`Events.TokenUsageStatus.ACTUAL`, `UNAVAILABLE`, or `null` when omitted.

`tokensUsed` in post-response and stop events must be an integral JSON number
within Java's signed 64-bit `long` range; `toolCallsCount` must fit a signed
32-bit `int`. Context counts and token values (`croppedCount`, `tokensBefore`,
`tokensAfter`, and `remainingTokens`) must be non-negative integral JSON numbers
that fit a signed 64-bit `long`. `usagePercent` must be finite and non-negative,
but is intentionally not capped at `1.0` because an overflow event can report a
larger value.

If an unknown notification arrives, or a known hook fails any required field,
enum, numeric, or range check, the callback receives `Events.UnknownEvent`.
`method()` retains the exact JSON-RPC method and `params()` retains the original
`JsonNode` top-level shape: object, array, JSON `null`, string, number, or
boolean. This keeps future CLI events and malformed payloads observable without
inventing wrapper fields.

```java
switch (event) {
    case Events.HookContextWarningEvent e ->
        System.out.println("Context usage: " + e.usagePercent());
    case Events.UnknownEvent e when e.method().startsWith("autohand.hook.") ->
        System.err.println("Unrecognized hook payload: " + e.params());
    default -> {}
}
```
