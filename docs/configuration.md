# Configuration

## Process provider selection

Set `SDKConfig.builder().provider(...)` to the canonical provider name (for example, `autohandai`).
The SDK forwards it as `AUTOHAND_PROVIDER` after other environment overrides.
With [CLI provider startup support](https://github.com/autohandai/code-cli/commit/240f071013316ebed4fcffb5af68f98cf2f8b2ff), this selects the provider ahead of global and workspace settings.
When no provider is configured or inferred by the SDK, normal CLI environment
and saved configuration selection apply. Older CLIs may ignore the override;
use a CLI containing the linked change.

Autohand AI inference credentials use `AUTOHAND_AI_API_KEY`,
`AUTOHAND_AI_BASE_URL`, and `AUTOHAND_AI_PLAN`. Account authentication is
separate, and configured feature gates still apply. The CLI retains saved
provider settings and credentials when other settings are saved during the run.

`SDKConfig` carries CLI startup configuration. Prefer the builder for new code:

```java
SDKConfig config = SDKConfig.builder()
    .cwd(".")
    .cliPath(System.getenv("AUTOHAND_CLI_PATH"))
    .model("openrouter/auto")
    .appendSystemPrompt("Prefer concise Java examples.")
    .addDirectory("../shared")
    .build();
```

The compatibility constructor is still supported:

```java
SDKConfig config = new SDKConfig(
    ".",                         // cwd
    System.getenv("AUTOHAND_CLI_PATH"),
    false,                       // debug
    300_000,                     // timeout ms
    "openrouter/auto"            // optional model in the compatibility tail
);
```

The Java SDK also accepts the long generated constructor shape used by the
cross-language examples, so existing examples can pass model, skill, context,
session, and AGENTS.md objects without source changes.

## Provider Setup

The SDK delegates provider calls to the Autohand CLI. Configure the CLI with
`~/.autohand/config.json`, or select the provider through `SDKConfig` using a
CLI with process provider selection support as described above.

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
