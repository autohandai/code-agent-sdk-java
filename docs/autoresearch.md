# Replayable Autoresearch Ledger

The Java SDK exposes the Autohand CLI's persisted autoresearch engine through
typed JSON-RPC records. A session benchmarks a baseline, proposes scoped
candidates, stores immutable measurements and decisions, and can later replay or
rescore those attempts without losing the original evidence.

## Start and drive a session

Use a clean Git repository with at least one commit. Check command support when
your application may connect to older CLI builds:

```java
if (!agent.supportsCommand("/autoresearch")) {
    throw new IllegalStateException("Upgrade the Autohand CLI first.");
}

Autoresearch.StartResult start = agent.startAutoresearch(
    Autoresearch.StartParams.builder("Reduce test runtime")
        .metricName("test_ms")
        .metricUnit("ms")
        .direction(Autoresearch.OptimizationDirection.LOWER)
        .measureCommand("mvn test")
        .checksCommand("mvn package")
        .maxIterations(3)
        .build()
);

if (!start.success() || start.instruction() == null) {
    throw new IllegalStateException(start.error());
}
agent.send(start.instruction()).waitForResult();
```

`startAutoresearch` persists the configuration and returns the instruction that
drives the autonomous experiment loop. `stopAutoresearch` pauses the loop; it
does not delete `.auto/` state.

## Inspect and replay evidence

The remaining methods map directly to the TypeScript 1.0.3 contract:

- `getAutoresearchStatus()` reads progress, run counts, attempts, and Pareto IDs.
- `getAutoresearchHistory()` lists attempts and their replay/materialization state.
- `replayAutoresearch(...)` evaluates a candidate with the original or current evaluator.
- `rescoreAutoresearch(...)` applies current decision policy to stored measurements.
- `compareAutoresearch(...)` compares two attempts' samples, checks, and decisions.
- `getAutoresearchPareto()` returns constraint-passing non-dominated candidates.
- `pinAutoresearch(...)` protects or releases a candidate's artifacts.
- `pruneAutoresearch(...)` previews retention by default; use `PruneParams.apply()` for explicit application.

Stream callbacks receive `Events.AutoresearchLifecycleEvent` for start/status/pause
and `Events.AutoresearchOperationEvent` for ledger operations. Unknown future
notifications remain available as `Events.UnknownEvent`.

See [`examples/AutoresearchLedger.java`](../examples/AutoresearchLedger.java) for
an end-to-end lifecycle that never pushes changes.
