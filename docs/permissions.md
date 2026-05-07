# Permissions

Autohand can request approval before running tools. Java surfaces those requests
as `Events.PermissionRequestEvent`.

```java
sdk.streamPrompt(new PromptParams("Run git status"), event -> {
    if (event instanceof Events.PermissionRequestEvent request) {
        sdk.allowPermission(request.requestId(), DecisionScope.ONCE);
    }
});
```

## Modes

```java
sdk.setPermissionMode(PermissionMode.INTERACTIVE);
sdk.setPermissionMode(PermissionMode.RESTRICTED);
sdk.setPermissionMode(PermissionMode.UNRESTRICTED);
```

## Decisions

```java
sdk.allowPermission(requestId, DecisionScope.ONCE);
sdk.allowPermission(requestId, DecisionScope.SESSION);
sdk.denyPermission(requestId, DecisionScope.ONCE);
sdk.permissionResponse(new PermissionResponseParams(
    requestId,
    PermissionDecision.ALTERNATIVE,
    false,
    "Run mvn test first",
    null
));
```
