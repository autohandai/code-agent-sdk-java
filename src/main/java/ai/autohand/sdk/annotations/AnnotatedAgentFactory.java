package ai.autohand.sdk.annotations;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.types.SkillReference;

import java.util.Arrays;

public final class AnnotatedAgentFactory {
    private AnnotatedAgentFactory() {
    }

    public static Agent create(Class<?> type) throws Exception {
        AutohandAgent annotation = type.getAnnotation(AutohandAgent.class);
        AgentOptions.Builder builder = AgentOptions.builder().cwd(".");
        if (annotation != null) {
            builder.model(annotation.model().isBlank() ? null : annotation.model());
            builder.instructions(annotation.instructions());
        }
        SystemPrompt systemPrompt = type.getAnnotation(SystemPrompt.class);
        if (systemPrompt != null) {
            if (systemPrompt.mode() == SystemPrompt.Mode.REPLACE) {
                builder.systemPrompt(systemPrompt.value());
            } else {
                builder.instructions(joinInstructions(annotation == null ? "" : annotation.instructions(), systemPrompt.value()));
            }
        }
        Permission permission = type.getAnnotation(Permission.class);
        if (permission != null) {
            builder.permissionMode(permission.value());
        }
        PlanMode planMode = type.getAnnotation(PlanMode.class);
        if (planMode != null) {
            builder.planMode(planMode.enabled());
        }
        Skills skills = type.getAnnotation(Skills.class);
        if (skills != null) {
            builder.skills(Arrays.stream(skills.value())
                    .map(name -> new SkillReference(name, null, null))
                    .toList());
        }
        return Agent.create(builder.build());
    }

    private static String joinInstructions(String first, String second) {
        if (first == null || first.isBlank()) {
            return second == null ? "" : second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + "\n\n" + second;
    }
}
