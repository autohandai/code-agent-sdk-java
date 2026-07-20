# Code Agent SDK for Java

Java SDK for controlling Autohand code agents through the CLI JSON-RPC mode.

**Beta:** this SDK is actively evolving while the Agent SDK APIs stabilize. Pin versions in production and review release notes before upgrading.

Official documentation: [https://autohand.ai/docs/agent-sdk/](https://autohand.ai/docs/agent-sdk/)

## Other Programming Languages (Beta)

The Agent SDK is available in multiple beta language packages. Use the same Autohand code-agent model from another programming language:

- [TypeScript](https://github.com/autohandai/code-agent-sdk-typescript) - `Agent`, `Run`, streaming, and JSON helpers for Node and Bun hosts.
- [Go](https://github.com/autohandai/code-agent-sdk-go) - idiomatic Go package with `context.Context`, typed events, and channel-based streaming.
- [Python](https://github.com/autohandai/code-agent-sdk-python) - async Python package with `async for` event streams and typed Pydantic models.
- [Java](https://github.com/autohandai/code-agent-sdk-java) - this package, with Java 21 records, sealed events, and virtual-thread-ready APIs.
- [Swift](https://github.com/autohandai/code-agent-sdk-swift) - SwiftPM package with `Agent`, `Runner`, async streams, tools, hooks, and permissions.
- [Rust](https://github.com/autohandai/code-agent-sdk-rust) - async Rust crate with Tokio, typed events, and stream-based runs.
- [C++](https://github.com/autohandai/code-agent-sdk-cpp) - modern C++20 package with CMake targets and typed event callbacks.
- [C#](https://github.com/autohandai/code-agent-sdk-csharp) - .NET package with `IAsyncEnumerable`, `CancellationToken`, and `System.Text.Json`.

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
    <version>1.0.0</version>
</dependency>
```

Maven Central releases are produced by the GitHub Actions release workflow. See
[Publishing To Maven Central](./docs/publishing.md) for the release checklist and
required repository secrets.

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

Current CLIs also expose command helpers and the typed replayable autoresearch
ledger:

```java
if (agent.supportsCommand("/autoresearch")) {
    Autoresearch.StartResult start = agent.startAutoresearch(
        Autoresearch.StartParams.builder("Reduce test runtime")
            .metricName("test_ms")
            .metricUnit("ms")
            .direction(Autoresearch.OptimizationDirection.LOWER)
            .measureCommand("mvn test")
            .build()
    );
    if (start.success() && start.instruction() != null) {
        agent.send(start.instruction()).waitForResult();
    }
    Autoresearch.HistoryResult history = agent.getAutoresearchHistory();
    agent.stopAutoresearch();
}
```

Read the [replayable autoresearch guide](./docs/autoresearch.md) for replay,
rescore, comparison, Pareto, pinning, and retention APIs.

Persistent goals are also typed end to end: `getGoal`, `createGoal`,
`updateGoal`, `clearGoal`, `queueGoal`, `startQueuedGoal`, and
`listGoalTemplates`. See the [persistent goals guide](./docs/persistent-goals.md)
for feature startup and nullable budget updates.

Community skills and MCP discovery are typed as well: `getSkillsRegistry`,
`installSkill`, `listMcpServers`, `listMcpTools`, and
`getMcpServerConfigs`. See the [skills and MCP guide](./docs/skills-and-mcp.md).

Use `reset()` to replace the active conversation and receive its new session ID.
Create an expiring browser attachment URL with `createBrowserHandoff(...)`.
Attach one by token with `attachBrowserHandoff(...)`.

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
- Spawns the Autohand CLI with `--mode rpc`
- Communicates via JSON-RPC 2.0 over stdin/stdout
- Provides an idiomatic Java API with builders, records, and sealed event types
- Streams events through Java callbacks while preserving typed permission replies
- Keeps future CLI notifications inspectable with `Events.UnknownEvent`

## Project Structure

```
src/main/java/ai/autohand/sdk/
  AutohandAgentSdk.java       # Entry point and version
  types/                      # Records, enums, sealed event types
    Event.java                  # Sealed event interface
    Events.java                 # Concrete event records
    SDKConfig.java              # Configuration builder and compatibility constructor
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

# Compile every example against the public API
scripts/validate-examples.sh

# Package
mvn package

# Enforce public-load and usable getState startup p95 < 50 ms
mvn -q -Dtest=StartupBenchmarkTest test

# Build Maven Central release artifacts locally (requires GPG)
mvn -P release verify
```

## Documentation

- [Agent SDK Docs](https://autohand.ai/docs/agent-sdk/)
- [Getting Started](./docs/getting-started.md)
- [API Reference](./docs/API_REFERENCE.md)
- [Configuration](./docs/configuration.md)
- [Community Skills and MCP Discovery](./docs/skills-and-mcp.md)
- [Startup Performance](./docs/performance.md)
- [Event Streaming](./docs/event-streaming.md)
- [Error Handling](./docs/error-handling.md)
- [Advanced Patterns](./docs/advanced-patterns.md)
- [Permissions](./docs/permissions.md)
- [Plan Mode](./docs/plan-mode.md)
- [Memory](./docs/memory.md)
- [Replayable Autoresearch](./docs/autoresearch.md)
- [Persistent Goals](./docs/persistent-goals.md)
- [SDLC Workflows](./docs/sdlc-workflows.md)
- [Publishing To Maven Central](./docs/publishing.md)
- [Contributing](./CONTRIBUTING.md)
- [SDLC Guide](./SDLC.md)

## License

[Apache License 2.0](./LICENSE)
