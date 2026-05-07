package ai.autohand.sdk.types;

/**
 * All available hook events in the CLI.
 *
 * @see HookDefinition
 * @see AutohandSDK#addHook(HookDefinition)
 */
public enum HookEvent {
    SESSION_START,
    SESSION_END,
    PRE_CLEAR,
    PRE_PROMPT,
    PRE_TOOL,
    POST_TOOL,
    FILE_MODIFIED,
    STOP,
    SUBAGENT_STOP,
    PERMISSION_REQUEST,
    NOTIFICATION,
    SESSION_ERROR,
    AUTOMODE_START,
    AUTOMODE_ITERATION,
    AUTOMODE_CHECKPOINT,
    AUTOMODE_PAUSE,
    AUTOMODE_RESUME,
    AUTOMODE_CANCEL,
    AUTOMODE_COMPLETE,
    AUTOMODE_ERROR,
    PRE_LEARN,
    POST_LEARN,
    TEAM_CREATED,
    TEAM_MATE_SPAWNED,
    TEAM_MATE_IDLE,
    TASK_ASSIGNED,
    TASK_COMPLETED,
    TEAM_SHUTDOWN,
    REVIEW_START,
    REVIEW_END,
    REVIEW_PAUSED,
    REVIEW_FAILED,
    REVIEW_COMPLETED,
    MODE_CHANGE,
    CONTEXT_COMPACT,
    CONTEXT_OVERFLOW,
    CONTEXT_WARNING,
    CONTEXT_CRITICAL;

    /**
     * Returns the CLI-compatible event name.
     * Maps underscore-separated names to hyphen or colon separated names.
     *
     * @return the event name formatted for CLI communication
     */
    public String toCliString() {
        String raw = name().toLowerCase().replace("_", "-");
        // Automode events use colon separator
        if (raw.startsWith("automode-")) {
            return raw.replaceFirst("automode-", "automode:");
        }
        // Review events use colon separator
        if (raw.startsWith("review-")) {
            return raw.replaceFirst("review-", "review:");
        }
        // Context events use colon separator
        if (raw.startsWith("context-")) {
            return raw.replaceFirst("context-", "context:");
        }
        return raw;
    }
}
