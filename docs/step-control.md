# Stop after completed tool steps

Use a CLI with `autohand.stepEnd` and `autohand.stepDecision` support. The CLI
bundles built from `9a878fe4` were used for Java integration verification.

```java
try (var agent = Agent.create(SDKConfig.builder()
        .provider("autohandai").model("fantail")
        .apiKey(System.getenv("AUTOHAND_AI_API_KEY"))
        .build())) {
    var result = agent.run(new PromptParams("Read README.md using read_file")
            .withStopWhen(StopConditions.isStepCount(1)));
    if (result.status().equals("stopped")) {
        System.out.println(result.steps().getFirst().toolResults());
        System.out.println(agent.run("Continue using the saved result").text());
    }
}
```

`isStepCount(n)` requires a positive count. `hasToolCall(name)` matches a tool in
the latest completed step. Multiple conditions are evaluated concurrently and
combined with OR. Each condition receives an immutable list of completed steps
for the current prompt and returns a `CompletionStage<Boolean>`:

```java
var prompt = new PromptParams("Inspect the project").withStopWhen(context ->
        CompletableFuture.completedFuture(context.steps().size() >= 3));
```

Callbacks stay in Java. Only `stopWhen: {mode: "host"}` goes over RPC. Steps
contain tool calls and the tool results already persisted by the CLI. Stopping
preserves that state; another prompt on the same agent continues from it.
`RunResult.steps()` is specific to each run. Status is `completed`, `stopped`, or
`aborted`; old four-argument RunResult and one-argument PromptParams constructors
remain available.

`send(prompt)` returns a Run. Consume it with `stream(...)` or `waitForResult()`;
another thread can call `run.abort()` while it runs. An unresolved asynchronous
condition does not block abort or CLI exit, and its later completion cannot send
a decision into a subsequent turn. Cancelling a queued run does not abort the
active run or send the queued prompt. A failing condition requests a safe stop and
then surfaces its failure. Malformed steps, rejected decisions, callback errors,
and lost transports fail the run; interrupted turns are aborted and drained
before another prompt starts, or their unresponsive subprocess is closed.

Plain `sdk.prompt(...)` and streamed prompts wait through the actual terminal
event. A prompt acknowledgement alone is not completion. Control RPCs remain
available while a prompt is running. `Agent.create(SDKConfig)` accepts the full
provider/environment/startup configuration; `Agent.create(AgentOptions)` remains
supported.

Run the fixture tests with Maven. To include compiled CLI provider, discovery,
tool persistence and resume proof using only local HTTP mocks:

```sh
AUTOHAND_TEST_CLI_PATH=/absolute/path/to/autohand mvn verify
```
