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
