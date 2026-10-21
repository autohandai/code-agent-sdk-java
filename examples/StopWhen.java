import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.types.PromptParams;
import ai.autohand.sdk.types.SDKConfig;
import ai.autohand.sdk.types.StopConditions;

/** Inspect a tool result before continuing the same Autohand AI session. */
public class StopWhen {
    public static void main(String[] args) throws Exception {
        try (var agent = Agent.create(SDKConfig.builder()
                .provider("autohandai").model("fantail")
                .apiKey(System.getenv("AUTOHAND_AI_API_KEY")).build())) {
            var result = agent.run(new PromptParams("Read README.md using read_file")
                    .withStopWhen(StopConditions.isStepCount(1)));
            System.out.println(result.status());
            result.steps().forEach(step -> System.out.println(step.toolResults()));
            if (result.status().equals("stopped")) {
                System.out.println(agent.run("Continue using the saved result").text());
            }
        }
    }
}
