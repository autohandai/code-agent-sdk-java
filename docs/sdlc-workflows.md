# SDLC Workflows With The Java SDK

## Discovery

```java
sdk.enablePlanMode();
sdk.streamPrompt(new PromptParams("Inspect this package and produce an implementation plan."), event -> {
    if (event instanceof Events.MessageUpdateEvent e) {
        System.out.print(e.delta());
    }
});
```

## Gated Implementation

```java
sdk.disablePlanMode();
sdk.setPermissionMode(PermissionMode.INTERACTIVE);
sdk.streamPrompt(new PromptParams("Implement the approved plan."), event -> {
    switch (event) {
        case Events.PermissionRequestEvent e -> sdk.allowPermission(e.requestId(), DecisionScope.ONCE);
        case Events.MessageUpdateEvent e -> System.out.print(e.delta());
        default -> {}
    }
});
```

## Release Readiness

```java
sdk.streamPrompt(new PromptParams("""
Run release readiness:
- mvn test
- inspect README and examples for API drift
- summarize residual risk
"""), event -> {
    if (event instanceof Events.MessageUpdateEvent e) {
        System.out.print(e.delta());
    }
});
```
