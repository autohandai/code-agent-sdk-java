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
- `agent.runJson(String prompt, Class<T> type)`: parses final output as JSON.
- `agent.runJson(String prompt, Class<T> type, String schemaName, Object schema, Map<String, Object> options)`: appends JSON output instructions and parses the final output.
- `agent.allowPermission(String requestId, DecisionScope scope)`: approves a permission request.
- `agent.denyPermission(String requestId, DecisionScope scope)`: denies a permission request.
- `agent.command(String command, String arguments)`: sends a slash command.
- `agent.deepResearch(String topic)`, `agent.autoresearch(String objective)`: current CLI command helpers.
- `agent.supportsCommand(String command)`: checks the connected CLI surface.
- Typed autoresearch lifecycle and ledger methods listed below are also available on `Agent`.
- Typed persistent-goal methods listed below are also available on `Agent`.
- Typed community-skill and MCP discovery methods listed below are also available on `Agent`.
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
- `prompt(PromptParams params)`: returns `PromptResult`
- `streamPrompt(PromptParams params, Consumer<Event> onEvent)`
- `setPermissionMode(PermissionMode mode)`
- `setPlanMode(boolean enabled)`, `enablePlanMode()`, `disablePlanMode()`
- `setModel(String model)`
- `setMaxThinkingTokens(int tokens)`
- `clearMaxThinkingTokens()`
- `applyFlagSettings(Map<String, Object> settings)`
- `supportedModels()`: returns `List<ModelInfo>`
- `supportedCommands()`: returns `List<String>`
- `supportsCommand(String command)`
- `getGoal()`, `createGoal(Goals.CreateParams params)`, `updateGoal(Goals.UpdateParams params)`
- `clearGoal()`, `queueGoal(Goals.CreateParams params)`, `startQueuedGoal()`, `listGoalTemplates()`
- `startAutoresearch(Autoresearch.StartParams params)`
- `getAutoresearchStatus()`, `stopAutoresearch()`, `getAutoresearchHistory()`
- `replayAutoresearch(...)`, `rescoreAutoresearch(...)`, `compareAutoresearch(...)`
- `getAutoresearchPareto()`, `pinAutoresearch(...)`, `pruneAutoresearch(...)`
- `getState()`, `getMessages()`
- `reset()`: replaces the active conversation and returns its new session ID.
- `createBrowserHandoff(BrowserHandoff.CreateParams params)`: creates an expiring browser attachment URL.
- `getSkillsRegistry()`, `getSkillsRegistry(CommunitySkills.RegistryParams params)`
- `installSkill(CommunitySkills.InstallParams params)`
- `getContextUsage()`: returns `ContextUsage`
- `accountInfo()`, `getAccountInfo()`: returns `AccountInfo`
- `reloadPlugins()`
- `toggleMcpServer(String serverName, boolean enabled)`
- `reconnectMcpServer(String serverName)`
- `setMcpServers(Map<String, McpServerConfig> servers)`
- `listMcpServers()`: returns `McpDiscovery.ListServersResult`
- `listMcpTools()`, `listMcpTools(McpDiscovery.ListToolsParams params)`
- `getMcpServerConfigs()`: returns `McpDiscovery.GetServerConfigsResult`
- `getHooks()`, `addHook(HookDefinition hook)`, `removeHook(HookEvent event, int index)`, `toggleHook(HookEvent event, int index)`
- `saveSession()`, `resumeSession(String sessionId)`
- `getSessionMetadata()`, `getStats()`
- `allowPermission(String requestId, DecisionScope scope)`
- `denyPermission(String requestId, DecisionScope scope)`
- `permissionResponse(PermissionResponseParams params)`

`Goals.NullableUpdate.unchanged()`, `.set(value)`, and `.clear()` preserve the
CLI's omitted/value/JSON-null distinction for mutable goal budgets.

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
- `FileModifiedEvent`
- `AutoresearchLifecycleEvent`
- `AutoresearchOperationEvent`
- `ErrorEvent`
- `UnknownEvent`

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
