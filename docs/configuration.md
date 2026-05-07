# Configuration

`SDKConfig` carries CLI startup configuration.

```java
SDKConfig config = new SDKConfig(
    ".",                         // cwd
    System.getenv("AUTOHAND_CLI_PATH"),
    false,                       // debug
    300_000,                     // timeout ms
    "openrouter/auto"            // optional model in the compatibility tail
);
```

The Java scaffold accepts the long generated constructor shape used by the
examples, so existing examples can pass provider, permission, skill, context,
session, and AGENTS.md objects without source changes.

## Provider Setup

The SDK delegates provider calls to the Autohand CLI. Configure the CLI with
`~/.autohand/config.json`, or pass model/provider hints through `SDKConfig` when
the CLI supports those startup flags.

## Common Settings

- `cwd`: workspace directory.
- `cliPath`: explicit Autohand CLI binary path.
- `debug`: log transport/RPC details.
- `timeoutMs`: request timeout in milliseconds.
- `PermissionSettings`: permission allow/deny preferences.
- `ContextSettings`: context compaction thresholds.
- `SessionSettings`: session persistence options.
- `AgentsMdSettings`: AGENTS.md loading behavior.
- `SkillReference`: skill names, paths, or inline content.
