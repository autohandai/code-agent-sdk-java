# Advanced Patterns

## Streaming Events

The Java SDK uses virtual threads for background I/O. Events are delivered through a `Consumer<Event>` callback:

```java
sdk.streamPrompt(params, event -> {
    switch (event) {
        case Events.MessageUpdateEvent mue -> System.out.print(mue.delta());
        case Events.ToolStartEvent tse -> logger.info("Tool: {}", tse.toolName());
        case Events.PermissionRequestEvent pre -> {
            // Auto-allow read-only tools
            if (pre.tool().equals("read_file") || pre.tool().equals("git_status")) {
                sdk.allowPermission(pre.requestId(), DecisionScope.SESSION);
            } else {
                sdk.denyPermission(pre.requestId(), DecisionScope.ONCE);
            }
        }
        default -> {}
    }
});
```

## Plan Mode

Plan mode restricts the agent to read-only planning tools:

```java
sdk.enablePlanMode();
sdk.streamPrompt(new PromptParams("Plan this refactor"), event -> { /* ... */ });
// Review the plan, then disable plan mode to execute
sdk.disablePlanMode();
```

## System Prompts

Replace the entire system prompt:

```java
AutohandSDK sdk = new AutohandSDK();
sdk.setSystemPrompt("./SYSTEM_PROMPT.md");
sdk.start();
```

Append to the default system prompt:

```java
sdk.appendSystemPrompt("Always run mvn verify before summarizing.");
sdk.start();
```

## Model Switching

Change the model at runtime:

```java
sdk.setModel("openrouter/anthropic/claude-sonnet-4");
```

## Hooks

Register hooks for extensibility:

```java
HookDefinition hook = new HookDefinition(
    HookEvent.PRE_TOOL,
    "echo 'Running tool: ${HOOK_TOOL}'",
    "Log tool usage",
    true,
    5000,
    true,
    null,
    null
);

sdk.addHook(hook);
```

## MCP Servers

Configure MCP servers dynamically:

```java
McpServerConfig filesystem = new McpServerConfig(
    "stdio",
    "npx",
    List.of("-y", "@modelcontextprotocol/server-filesystem", "/path/to/files"),
    null, null, null, true
);

sdk.setMcpServers(Map.of("filesystem", filesystem));
```

## Context Usage

Monitor context window usage:

```java
ContextUsage usage = sdk.getContextUsage();
System.out.println("Total: " + usage.total() + " tokens");
System.out.println("System prompt: " + usage.systemPrompt() + " tokens");
```

## Error Handling

The SDK uses structured exceptions:

```java
try {
    sdk.start();
} catch (IOException e) {
    System.err.println("Failed to start CLI: " + e.getMessage());
}

try {
    var result = agent.runJson("Return JSON", MyClass.class);
} catch (StructuredOutputError e) {
    System.err.println("Invalid JSON: " + e.getMessage());
    System.err.println("Raw: " + e.rawResponse());
}
```

## Multi-turn Conversations

Keep the same agent alive across multiple prompts:

```java
Agent agent = Agent.create(options);

try {
    agent.run("First task");
    agent.run("Second task");
    agent.run("Third task");
} finally {
    agent.close();
}
```

## Session Management

Persist and resume sessions:

```java
SDKConfig config = new SDKConfig(
    ".", null, false, 300_000,
    null, null, null, null, null,
    null, null, null, null,
    null, null, null, null,
    null, null, null, null, null, null,
    null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null,
    null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null
);
// Use session settings in config
```
