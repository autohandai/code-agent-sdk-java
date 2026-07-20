# Startup Performance

The regression benchmark measures three different costs:

1. A fresh child JVM times loading the public `AutohandAgentSdk` entry point
   from inside the process, excluding JVM boot.
2. An already-running benchmark JVM times the public `start()` API returning
   after it has spawned the deterministic fixture.
3. The same ready JVM times fixture spawn through a successful typed
   `getState` response.

All three use 5 warmups followed by 50 measured samples and fail when p95 is not
below 50 ms. Run them with:

```bash
mvn -q -Dtest=StartupBenchmarkTest test
```

Measured on 2026-07-20 on the development macOS host:

```json
{
  "language" : "java",
  "budgetMs" : 50.0,
  "metrics" : {
    "publicImportMs" : {
      "samples" : 50,
      "medianMs" : 1.564,
      "p95Ms" : 3.314,
      "maxMs" : 3.859,
      "passed" : true
    },
    "sdkStartReturnMs" : {
      "samples" : 50,
      "medianMs" : 2.716,
      "p95Ms" : 4.809,
      "maxMs" : 5.615,
      "passed" : true
    },
    "fixtureSpawnToFirstRpcMs" : {
      "samples" : 50,
      "medianMs" : 4.286,
      "p95Ms" : 7.206,
      "maxMs" : 7.619,
      "passed" : true
    }
  },
  "passed" : true
}
```

The benchmark uses a compiled local C JSON-RPC fixture so it measures the SDK
transport path without provider or network latency. Host load still affects
process-spawn measurements; CI reruns the same threshold gate.
