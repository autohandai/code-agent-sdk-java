# Error Handling

The Java SDK uses checked exceptions for startup and structured-output failures,
and emits agent-loop failures as `Events.ErrorEvent`.

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
