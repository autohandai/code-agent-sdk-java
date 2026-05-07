package ai.autohand.sdk.types;

public record SessionSettings(
        boolean persist,
        boolean resume,
        String sessionPath,
        int autoSaveInterval
) {
}
