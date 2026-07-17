package ai.autohand.sdk.types;

/** CLI skill discovery roots. */
public enum SkillSource {
    CODEX_USER("codex-user"),
    CODEX_PROJECT("codex-project"),
    CLAUDE_USER("claude-user"),
    CLAUDE_PROJECT("claude-project"),
    AUTOHAND_USER("autohand-user"),
    AUTOHAND_PROJECT("autohand-project"),
    COMMUNITY("community");

    private final String cliValue;

    SkillSource(String cliValue) {
        this.cliValue = cliValue;
    }

    public String cliValue() {
        return cliValue;
    }
}
