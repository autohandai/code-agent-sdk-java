# Contributing

Thanks for helping improve the Autohand Code Agent SDK for Java. This repository
is open source, and we want it to feel good for both application developers and
people maintaining the SDK.

## Useful Links

- Agent SDK docs: [https://autohand.ai/docs/agent-sdk/](https://autohand.ai/docs/agent-sdk/)
- Autohand Code CLI: [https://github.com/autohandai/code-cli](https://github.com/autohandai/code-cli)
- TypeScript baseline SDK: [https://github.com/autohandai/code-agent-sdk-typescript](https://github.com/autohandai/code-agent-sdk-typescript)

## Development Setup

Requirements:

- Java 21 or later
- Maven 3.9 or later
- Autohand CLI installed for live examples

```bash
mvn test
scripts/validate-examples.sh
```

`mvn test` includes transport/RPC tests using a fake CLI and compiles every
example under `examples/`. `scripts/validate-examples.sh` is the standalone
example gate used when you only want to verify copy-paste source compatibility.

## API Design Expectations

- Keep the high-level `Agent` / `Run` API pleasant for normal application code.
- Keep low-level `AutohandSDK` methods close to the TypeScript SDK and CLI
  JSON-RPC names so behavior is easy to compare across languages.
- Prefer Java 21 records, sealed interfaces, builders, and pattern matching where
  they make the API clearer.
- Treat examples as part of the public contract. If an example changes, compile
  it and make sure the README/docs still match.
- Preserve forward compatibility for new CLI events with `Events.UnknownEvent`
  unless a typed event is needed.

## Commit Messages

Use Conventional Commits:

```text
<type>[optional scope]: <description>
```

Common types:

- `feat`: new SDK feature
- `fix`: bug fix
- `docs`: documentation only
- `test`: tests or example validation
- `refactor`: internal restructuring without behavior changes
- `chore`: maintenance

Examples:

```bash
git commit -m "feat(rpc): add typed hook controls"
git commit -m "test(examples): compile all Java examples"
git commit -m "docs: add getting started guide"
```

## Pull Request Checklist

- Tests pass with `mvn test`.
- Examples compile with `scripts/validate-examples.sh`.
- README and docs are updated for user-facing API changes.
- New public methods have at least one example or focused test.
- No API keys, personal data, or local machine paths are committed.

## Reporting Issues

Use the bug report or feature request templates. Include Java version, Maven
version, SDK version or commit, Autohand CLI version, operating system, and the
smallest reproduction you can share.
