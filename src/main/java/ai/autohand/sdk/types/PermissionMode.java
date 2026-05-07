package ai.autohand.sdk.types;

public enum PermissionMode {
    INTERACTIVE("interactive"),
    RESTRICTED("restricted"),
    UNRESTRICTED("unrestricted"),
    EXTERNAL("external");

    private final String cliValue;

    PermissionMode(String cliValue) {
        this.cliValue = cliValue;
    }

    public String cliValue() {
        return cliValue;
    }
}
