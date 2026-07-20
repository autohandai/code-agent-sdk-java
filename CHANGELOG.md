# Changelog

## Unreleased

### Added

- Typed community skill registry and installation APIs.
- Typed MCP server, tool, and server-configuration discovery APIs.
- Reproducible public-package-load and spawn-through-`getState` performance gates.

### Fixed

- Dispatch responses centrally so control RPCs can complete during an active prompt while
  event-bearing prompts remain serialized and notifications cannot cross streams.
- Treat callback failures as terminal run failures instead of resending the prompt.
- Roll back the subprocess when startup configuration fails.
- Wake every in-flight request immediately when the transport closes or reaches stdout EOF.
- Ignore stale stdout and close signals from previous transport generations after a restart.
- Discard timed-out pending responses so late replies cannot leak or poison later requests.
- Decode conversation history as typed `RpcMessage` values, including tool calls.
