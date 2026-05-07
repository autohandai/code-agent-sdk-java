package ai.autohand.sdk;

import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutohandSdkTest {
    @Test
    void createsHighLevelAgent() throws Exception {
        try (Agent agent = Agent.create(AgentOptions.builder().cwd(".").build())) {
            assertEquals(".", agent.options().cwd());
        }
    }
}
