package ai.autohand.sdk.types;

import java.util.Map;

/** Runtime feature settings applied after the RPC subprocess starts. */
public record FeatureFlagSettings(
        String environment,
        Map<String, String> remoteOverrides,
        Boolean usageV2,
        Boolean awsBedrockProvider,
        Boolean slashGoal,
        Boolean tokenUsageStatus,
        Boolean experimentalFork,
        Boolean experimentalClone,
        Boolean experimentalHandoff
) {
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for sparse feature settings. */
    public static final class Builder {
        private String environment;
        private Map<String, String> remoteOverrides;
        private Boolean usageV2;
        private Boolean awsBedrockProvider;
        private Boolean slashGoal;
        private Boolean tokenUsageStatus;
        private Boolean experimentalFork;
        private Boolean experimentalClone;
        private Boolean experimentalHandoff;

        public Builder environment(String value) { environment = value; return this; }
        public Builder remoteOverrides(Map<String, String> value) { remoteOverrides = value; return this; }
        public Builder usageV2(Boolean value) { usageV2 = value; return this; }
        public Builder awsBedrockProvider(Boolean value) { awsBedrockProvider = value; return this; }
        public Builder slashGoal(Boolean value) { slashGoal = value; return this; }
        public Builder tokenUsageStatus(Boolean value) { tokenUsageStatus = value; return this; }
        public Builder experimentalFork(Boolean value) { experimentalFork = value; return this; }
        public Builder experimentalClone(Boolean value) { experimentalClone = value; return this; }
        public Builder experimentalHandoff(Boolean value) { experimentalHandoff = value; return this; }

        public FeatureFlagSettings build() {
            return new FeatureFlagSettings(environment, remoteOverrides, usageV2, awsBedrockProvider,
                    slashGoal, tokenUsageStatus, experimentalFork, experimentalClone, experimentalHandoff);
        }
    }
}
