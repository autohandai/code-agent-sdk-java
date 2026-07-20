# Community Skills and MCP Discovery

The SDK exposes the current CLI registry and MCP discovery methods with typed
request and response records.

```java
CommunitySkills.RegistryResult registry = sdk.getSkillsRegistry(
    new CommunitySkills.RegistryParams(true));

CommunitySkills.InstallResult installed = sdk.installSkill(
    new CommunitySkills.InstallParams(
        "java-quality",
        CommunitySkills.Scope.PROJECT,
        true));

McpDiscovery.ListServersResult servers = sdk.listMcpServers();
McpDiscovery.ListToolsResult tools = sdk.listMcpTools(
    new McpDiscovery.ListToolsParams("github"));
McpDiscovery.GetServerConfigsResult configs = sdk.getMcpServerConfigs();
```

`RegistryParams.cached()` leaves `forceRefresh` absent. Likewise,
`ListToolsParams.allServers()` leaves `serverName` absent so the CLI returns
tools from every configured server. Installation scope is explicit:
`USER` writes to the user skill directory and `PROJECT` writes to the current
project.
