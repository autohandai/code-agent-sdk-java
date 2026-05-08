# SDLC Guide

This repository follows a small, evidence-driven workflow so the Java SDK stays
pleasant to use and compatible with the Autohand CLI.

## 1. Discover

- Compare behavior with the TypeScript SDK when adding cross-language features.
- Check the CLI JSON-RPC method name and payload before designing Java wrappers.
- Decide whether the change belongs in high-level `Agent` / `Run`, low-level
  `AutohandSDK`, or both.

## 2. Design

- Prefer typed Java records and enums over unstructured maps for stable behavior.
- Keep escape hatches for forward compatibility, especially `Events.UnknownEvent`
  and raw RPC result handling.
- Make the most common path short and readable, then expose lower-level controls
  for advanced users.

## 3. Implement

- Keep transport, RPC mapping, public SDK methods, and examples aligned.
- Avoid changing generated or existing examples without compiling all examples.
- Keep docs honest about what is implemented and what requires a live CLI.

## 4. Verify

Run:

```bash
mvn test
scripts/validate-examples.sh
```

For live examples, set `AUTOHAND_CLI_PATH` when the CLI is not on `PATH`:

```bash
AUTOHAND_CLI_PATH=/path/to/autohand mvn compile exec:java -Dexec.mainClass="HighLevelAgent"
```

## 5. Release Readiness

- Confirm README, docs, and examples match the public API.
- Confirm issue templates and community files are present.
- Confirm GitHub repository metadata points to
  [https://autohand.ai/docs/agent-sdk/](https://autohand.ai/docs/agent-sdk/).
- Use Conventional Commits so release automation can classify changes.
