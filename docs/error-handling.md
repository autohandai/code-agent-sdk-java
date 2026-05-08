# Error Handling

The Java SDK uses checked exceptions for startup and structured-output parsing,
unchecked `AutohandException` subclasses for transport/RPC failures, and
`Events.ErrorEvent` for agent-loop failures emitted by the CLI.

## Startup And Transport

```java
try {
    sdk.start();
} catch (Exception error) {
    System.err.println("Failed to start Autohand: " + error.getMessage());
}
```

Common causes:

- `AUTOHAND_CLI_PATH` points to a missing binary.
- The CLI has no configured provider.
- The workspace path is invalid.

## RPC And Timeout Errors

```java
try {
    sdk.streamPrompt(new PromptParams("Run checks"), System.out::println);
} catch (RequestTimeoutException timeout) {
    System.err.println("Timed out calling " + timeout.method());
} catch (RpcException rpc) {
    System.err.println("RPC failed: " + rpc.method() + " code=" + rpc.code());
}
```

## Stream Errors

```java
sdk.streamPrompt(new PromptParams("Run checks"), event -> {
    if (event instanceof Events.ErrorEvent e) {
        System.err.println("Agent error: " + e.message());
    }
});
```

## Structured Output

```java
try {
    ReleaseRisk risk = agent.runJson("Return release risk as JSON", ReleaseRisk.class);
} catch (StructuredOutputError error) {
    System.err.println(error.getRawResponsePreview());
}
```
