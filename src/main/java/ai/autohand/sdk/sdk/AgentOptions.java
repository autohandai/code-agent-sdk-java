package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.PermissionMode;

public record AgentOptions(String cwd, String cliPath, String instructions, PermissionMode permissionMode, String model) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String cwd = ".";
        private String cliPath;
        private String instructions = "";
        private PermissionMode permissionMode = PermissionMode.INTERACTIVE;
        private String model;

        public Builder cwd(String cwd) {
            this.cwd = cwd;
            return this;
        }

        public Builder cliPath(String cliPath) {
            this.cliPath = cliPath;
            return this;
        }

        public Builder instructions(String instructions) {
            this.instructions = instructions;
            return this;
        }

        public Builder permissionMode(PermissionMode permissionMode) {
            this.permissionMode = permissionMode;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public AgentOptions build() {
            return new AgentOptions(cwd, cliPath, instructions, permissionMode, model);
        }
    }
}
