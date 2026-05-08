# Getting Started with Autohand Java SDK

## Prerequisites

1. Java 21 or later installed
2. Maven 3.9 or later
3. Autohand CLI binary available on your system or in the `cli/` directory

Official Agent SDK docs: [https://autohand.ai/docs/agent-sdk/](https://autohand.ai/docs/agent-sdk/)

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.autohand</groupId>
    <artifactId>code-agent-sdk-java</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Local Build

```bash
cd java/
mvn clean install
```

## Configuration

### SDK Configuration

```java
SDKConfig config = SDKConfig.builder()
    .cwd(".")
    .cliPath(System.getenv("AUTOHAND_CLI_PATH"))
    .model("openrouter/auto")
    .appendSystemPrompt("Prefer concise Java examples.")
    .build();
```

### CLI Configuration

The SDK uses the CLI's configuration file (`~/.autohand/config.json`). You can configure providers there:

```json
{
  "provider": "openrouter",
  "openrouter": {
    "apiKey": "sk-or-...",
    "model": "openrouter/auto"
  }
}
```

## Your First Agent

```java
import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.types.*;

public class HelloAgent {
    public static void main(String[] args) throws Exception {
        Agent agent = Agent.create(AgentOptions.builder()
            .cwd(".")
            .model("openrouter/auto")
            .instructions("Be concise and helpful.")
            .build());

        try {
            var run = agent.send("Explain what this SDK does");

            run.stream(event -> {
                if (event instanceof Events.MessageUpdateEvent mue) {
                    System.out.print(mue.delta());
                }
            });

            System.out.println("\nDone!");
        } finally {
            agent.close();
        }
    }
}
```

## Event Types

The SDK emits the following events via sealed interface `Event`:

- `AgentStartEvent` - Agent started a session
- `AgentEndEvent` - Agent ended a session
- `TurnStartEvent` - Turn started
- `TurnEndEvent` - Turn ended
- `MessageStartEvent` - Message generation started
- `MessageUpdateEvent` - Message content update (streaming)
- `MessageEndEvent` - Message generation ended
- `ToolStartEvent` - Tool execution started
- `ToolUpdateEvent` - Tool output update (streaming)
- `ToolEndEvent` - Tool execution ended
- `FileModifiedEvent` - File was modified
- `PermissionRequestEvent` - Permission request from agent
- `ErrorEvent` - Error occurred

Use pattern matching to handle events:

```java
run.stream(event -> {
    switch (event) {
        case Events.MessageUpdateEvent mue -> System.out.print(mue.delta());
        case Events.ToolStartEvent tse -> System.out.println("Running: " + tse.toolName());
        case Events.PermissionRequestEvent pre -> handlePermission(pre);
        default -> {}
    }
});
```

## Permission Handling

```java
sdk.streamPrompt(params, event -> {
    if (event instanceof Events.PermissionRequestEvent pre) {
        System.out.println("Allow " + pre.tool() + "?");
        sdk.allowPermission(pre.requestId(), DecisionScope.SESSION);
    }
});
```

Ergonomic helpers:

```java
sdk.allowPermission(requestId, DecisionScope.ONCE);
sdk.denyPermission(requestId, DecisionScope.SESSION);
sdk.suggestPermissionAlternative(requestId, "Run mvn test first");
```

## Next Steps

- See `examples/` for more examples
- Read `docs/advanced-patterns.md` for streaming, plan mode, and hooks
- Run `scripts/validate-examples.sh` before changing example code
