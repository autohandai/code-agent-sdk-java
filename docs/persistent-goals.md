# Persistent Goals

The Java SDK exposes the CLI's persistent goal ledger through seven typed
JSON-RPC operations. Enable the current goal surface at startup, then use the
same API from `AutohandSDK` or `Agent`:

```java
FeatureFlagSettings features = FeatureFlagSettings.builder()
    .slashGoal(true)
    .tokenUsageStatus(true)
    .build();

SDKConfig config = SDKConfig.builder()
    .features(features)
    .build();

try (AutohandSDK sdk = new AutohandSDK(config)) {
    sdk.start();
    sdk.createGoal(new Goals.CreateParams(
        "Ship the Java SDK",
        new Goals.Budget(50_000L, 3_600L, 2_000L, 120L)
    ));
}
```

## Operations

- `getGoal()` reads the active goal, queue, and completed history.
- `createGoal(...)` creates or replaces the active goal.
- `updateGoal(...)` changes the objective, status, or budgets.
- `clearGoal()` completes and clears the current goal.
- `queueGoal(...)` appends work behind the current goal.
- `startQueuedGoal()` starts the next queued goal.
- `listGoalTemplates()` discovers CLI goal templates.

Budget fields on `Goals.UpdateParams` use `Goals.NullableUpdate`: `unchanged()`
omits the RPC field, `set(value)` sends a value, and `clear()` sends JSON `null`.
This preserves the CLI's three distinct update operations.

If the goal feature is disabled, `Goals.SnapshotResult.enabled()` and
`Goals.TemplatesResult.enabled()` are false and their `message()` explains the
required feature setting. Mutation results carry the CLI's `ok` and `message`
fields directly.
