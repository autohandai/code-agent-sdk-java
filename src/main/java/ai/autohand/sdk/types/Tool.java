package ai.autohand.sdk.types;

public enum Tool {
    READ_FILE("read_file"),
    WRITE_FILE("write_file"),
    APPEND_FILE("append_file"),
    APPLY_PATCH("apply_patch"),
    RUN_COMMAND("run_command"),
    GIT_STATUS("git_status"),
    GIT_DIFF("git_diff"),
    GIT_LOG("git_log"),
    SAVE_MEMORY("save_memory"),
    RECALL_MEMORY("recall_memory");

    private final String cliName;

    Tool(String cliName) {
        this.cliName = cliName;
    }

    public String cliName() {
        return cliName;
    }
}
