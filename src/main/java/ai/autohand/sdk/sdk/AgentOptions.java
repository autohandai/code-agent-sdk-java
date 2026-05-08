package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.PermissionMode;
import ai.autohand.sdk.types.SkillReference;

import java.util.List;

public record AgentOptions(
        String cwd,
        String cliPath,
        String instructions,
        PermissionMode permissionMode,
        String model,
        List<SkillReference> skills,
        boolean planMode,
        String systemPrompt
) {
    public AgentOptions(String cwd, String cliPath, String instructions, PermissionMode permissionMode, String model) {
        this(cwd, cliPath, instructions, permissionMode, model, List.of(), false, null);
    }

    public AgentOptions {
        cwd = cwd == null || cwd.isBlank() ? "." : cwd;
        instructions = instructions == null ? "" : instructions;
        permissionMode = permissionMode == null ? PermissionMode.INTERACTIVE : permissionMode;
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String cwd = ".";
        private String cliPath;
        private String instructions = "";
        private PermissionMode permissionMode = PermissionMode.INTERACTIVE;
        private String model;
        private List<SkillReference> skills = List.of();
        private boolean planMode;
        private String systemPrompt;

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

        public Builder skills(List<SkillReference> skills) {
            this.skills = skills;
            return this;
        }

        public Builder planMode(boolean planMode) {
            this.planMode = planMode;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public AgentOptions build() {
            return new AgentOptions(cwd, cliPath, instructions, permissionMode, model, skills, planMode, systemPrompt);
        }
    }
}
