package ai.autohand.sdk.types;

public record SessionStats(int turns, int messages, int toolCalls, double costUsd) {
}
