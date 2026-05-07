package ai.autohand.sdk.annotations;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;

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
        return Agent.create(builder.build());
    }
}
