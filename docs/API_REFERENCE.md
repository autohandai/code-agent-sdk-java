# API Reference

Complete reference for the Java Autohand Code Agent SDK.

## High-Level API

```java
Agent agent = Agent.create(AgentOptions.builder()
    .cwd(".")
    .instructions("Review code with Staff-level Java judgement.")
    .permissionMode(PermissionMode.INTERACTIVE)
    .build());
```

### Agent

- `Agent.create(AgentOptions options)`: starts a session.
- `agent.send(String prompt)`: creates a `Run`.
- `agent.run(String prompt)`: waits for a completed `RunResult`.
- `agent.runJson(String prompt, Class<T> type)`: parses final output as JSON when a validator/parser is available.
- `agent.allowPermission(String requestId, DecisionScope scope)`: approves a permission request.
- `agent.denyPermission(String requestId, DecisionScope scope)`: denies a permission request.
- `agent.close()`: stops the session.

### Run

- `run.stream(Consumer<Event> onEvent)`: streams events to a callback.
- `run.waitForResult()`: returns `RunResult`.
- `run.json(Class<T> type)`: parses structured output.

### RunResult

```java
public record RunResult(String id, String status, String text, List<Event> events) {}
```

## Low-Level API

```java
AutohandSDK sdk = new AutohandSDK(new SDKConfig(".", cliPath, false, 300_000));
sdk.start();
sdk.streamPrompt(new PromptParams("Analyze this package"), event -> {
    if (event instanceof Events.MessageUpdateEvent mue) {
        System.out.print(mue.delta());
    }
});
sdk.stop();
```

### AutohandSDK

- `start()`, `stop()`, `close()`
- `prompt(PromptParams params)`
- `streamPrompt(PromptParams params, Consumer<Event> onEvent)`
- `setPermissionMode(PermissionMode mode)`
- `setPlanMode(boolean enabled)`, `enablePlanMode()`, `disablePlanMode()`
- `setModel(String model)`
- `setMaxThinkingTokens(int tokens)`
- `applyFlagSettings(Map<String, Object> settings)`
- `supportedModels()`
- `getContextUsage()`
- `accountInfo()`
- `saveSession()`, `resumeSession(String sessionId)`
- `getSessionMetadata()`, `getStats()`
- `allowPermission(String requestId, DecisionScope scope)`
- `denyPermission(String requestId, DecisionScope scope)`
- `permissionResponse(PermissionResponseParams params)`

## Events

Events implement the `Event` marker interface and are exposed as records under
`Events`:

- `AgentStartEvent`
- `AgentEndEvent`
- `TurnStartEvent`
- `TurnEndEvent`
- `MessageStartEvent`
- `MessageUpdateEvent`
- `MessageEndEvent`
- `ToolStartEvent`
- `ToolUpdateEvent`
- `ToolEndEvent`
- `PermissionRequestEvent`
- `ErrorEvent`

Use Java 21 pattern matching:

```java
switch (event) {
    case Events.MessageUpdateEvent e -> System.out.print(e.delta());
    case Events.ToolStartEvent e -> System.out.println("Tool: " + e.toolName());
    case Events.PermissionRequestEvent e -> sdk.allowPermission(e.requestId(), DecisionScope.ONCE);
    case Events.ErrorEvent e -> System.err.println(e.message());
    default -> {}
}
```
