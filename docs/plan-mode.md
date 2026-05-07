# Plan Mode

Plan mode is for read-only discovery and implementation planning.

```java
sdk.enablePlanMode();
sdk.streamPrompt(new PromptParams("Plan this refactor. Do not edit files."), event -> {
    if (event instanceof Events.MessageUpdateEvent e) {
        System.out.print(e.delta());
    }
});
sdk.disablePlanMode();
```

For gated workflows, run a planning pass first, review the result in your host,
then execute in a separate session with interactive permissions.
