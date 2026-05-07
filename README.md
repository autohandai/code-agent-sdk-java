# Code Agent SDK for Java

Java SDK for controlling Autohand code agents through the CLI JSON-RPC mode.

## Other SDKs

Use the same Autohand code-agent model from another language:

- [TypeScript](https://github.com/autohandai/code-agent-sdk-typescript) - `Agent`, `Run`, streaming, and JSON helpers for Node and Bun hosts.
- [Go](https://github.com/autohandai/code-agent-sdk-go) - idiomatic Go package with `context.Context`, typed events, and channel-based streaming.
- [Python](https://github.com/autohandai/code-agent-sdk-python) - async Python package with `async for` event streams and typed Pydantic models.
- [Java](https://github.com/autohandai/code-agent-sdk-java) - this package, with Java 21 records, sealed events, and virtual-thread-ready APIs.
- [Swift](https://github.com/autohandai/code-agent-sdk-swift) - SwiftPM package with `Agent`, `Runner`, async streams, tools, hooks, and permissions.

## Requirements

- Java 21+ (uses virtual threads and sealed interfaces)
- Maven 3.9+
- Autohand CLI binary

## Installation

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>ai.autohand</groupId>
    <artifactId>code-agent-sdk-java</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Or build locally:

```bash
mvn clean install
```

## Quick Start

### High-Level API (Recommended)

Use `Agent` for application code. It gives you an explicit run lifecycle while keeping CLI subprocess and JSON-RPC details out of your app.

```java
import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.RunResult;
import ai.autohand.sdk.types.*;

Agent agent = Agent.create(AgentOptions.builder()
    .cwd(".")
    .instructions("Review code with Staff-level Java judgement.")
    .permissionMode(PermissionMode.INTERACTIVE)
    .build());

var run = agent.send("Review this repository for release readiness");

run.stream(event -> {
    if (event instanceof Events.MessageUpdateEvent mue) {
        System.out.print(mue.delta());
    }
});

RunResult result = run.waitForResult();
System.out.println(result.text());

agent.close();
```

For simple one-shot tasks:

```java
RunResult result = agent.run("Summarize the API surface");
```

For JSON output:

```java
public record ReleaseRisk(String summary, List<Risk> risks) {}
public record Risk(String title, String severity) {}

ReleaseRisk risk = agent.runJson(
    "Assess publish readiness",
    ReleaseRisk.class,
    "ReleaseRisk",
    Map.of("summary", "string", "risks", List.of(Map.of("title", "string", "severity", "low | medium | high"))),
    null
);
```

### Low-Level API

Use `AutohandSDK` when you need direct control over the CLI subprocess.

```java
import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.*;

AutohandSDK sdk = new AutohandSDK(new SDKConfig(
    ".",               // cwd
    null,              // cliPath (auto-detected)
    false,             // debug
    300_000            // timeout ms
));

sdk.start();

sdk.prompt(new PromptParams("Hello, Autohand!"));

sdk.streamPrompt(new PromptParams("Analyze the codebase"), event -> {
    System.out.println(event);
});

sdk.stop();
```

## Architecture

```
User -> Java SDK -> CLI Subprocess (RPC mode) -> Provider -> HTTP
```

The SDK:
- Spawns the Autohand CLI as a subprocess
- Communicates via JSON-RPC 2.0 over stdin/stdout
- Provides an idiomatic Java API with sealed event types
- Supports streaming events using virtual threads

## Project Structure

```
src/main/java/ai/autohand/sdk/
  AutohandAgentSdk.java       # Entry point and version
  types/                      # Records, enums, sealed event types
    Event.java                  # Sealed event interface
    Events.java                 # Concrete event records
    SDKConfig.java              # Configuration record
    PromptParams.java           # Prompt parameters
    ...
  transport/
    Transport.java              # Subprocess spawning and I/O
    TransportConfig.java        # Transport configuration
  rpc/
    RPCClient.java              # JSON-RPC client
  sdk/
    AutohandSDK.java            # Main SDK class
    Agent.java                   # High-level agent API
    Run.java                     # Run lifecycle
    RunResult.java               # Run result record
    JsonParser.java              # JSON parsing utilities
    StructuredOutputError.java   # Structured output error
```

## Development

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package
```

## Documentation

- [Getting Started](./docs/getting-started.md)
- [API Reference](./docs/API_REFERENCE.md)
- [Configuration](./docs/configuration.md)
- [Event Streaming](./docs/event-streaming.md)
- [Error Handling](./docs/error-handling.md)
- [Advanced Patterns](./docs/advanced-patterns.md)
- [Permissions](./docs/permissions.md)
- [Plan Mode](./docs/plan-mode.md)
- [Memory](./docs/memory.md)
- [SDLC Workflows](./docs/sdlc-workflows.md)

## License

Apache License 2.0
