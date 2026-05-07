package ai.autohand.sdk.types;

public enum PermissionDecision {
    ALLOW_ONCE("allow_once"),
    DENY_ONCE("deny_once"),
    ALLOW_SESSION("allow_session"),
    DENY_SESSION("deny_session"),
    ALLOW_ALWAYS_PROJECT("allow_always_project"),
    ALLOW_ALWAYS_USER("allow_always_user"),
    DENY_ALWAYS_PROJECT("deny_always_project"),
    DENY_ALWAYS_USER("deny_always_user"),
    ALTERNATIVE("alternative");

    private final String cliValue;

    PermissionDecision(String cliValue) {
        this.cliValue = cliValue;
    }

    public String cliValue() {
        return cliValue;
    }
}
