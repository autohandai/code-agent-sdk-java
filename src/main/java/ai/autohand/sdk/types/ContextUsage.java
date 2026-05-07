package ai.autohand.sdk.types;

/**
 * Breakdown of current context window usage in tokens.
 *
 * @param systemPrompt tokens used by the system prompt
 * @param tools tokens used by tool definitions
 * @param messages tokens used by conversation messages
 * @param mcpTools tokens used by MCP tool definitions
 * @param memoryFiles tokens used by loaded memory files
 * @param total total tokens used
 * @see AutohandSDK#getContextUsage()
 */
public record ContextUsage(
    int systemPrompt,
    int tools,
    int messages,
    int mcpTools,
    int memoryFiles,
    int total
) {
}
