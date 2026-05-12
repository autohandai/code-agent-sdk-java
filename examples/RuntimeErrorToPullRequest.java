import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.RunResult;
import ai.autohand.sdk.types.Events;

/**
 * Runtime error to pull request.
 *
 * This example shows how an application can capture a runtime error and ask
 * Autohand to fix it instead of only logging it. Point AUTOHAND_TARGET_REPO at
 * the application repository that should receive the branch, commit, push, and
 * pull request.
 *
 * Run with:
 *   AUTOHAND_TARGET_REPO=/path/to/app mvn compile exec:java -Dexec.mainClass="RuntimeErrorToPullRequest"
 */
public class RuntimeErrorToPullRequest {
    record Cart(double subtotal, Customer customer) {}
    record Customer(String loyaltyTier) {}

    public static void main(String[] args) throws Exception {
        String targetRepo = getenvOrDefault("AUTOHAND_TARGET_REPO", ".");
        String cliPath = System.getenv("AUTOHAND_CLI_PATH");
        String model = System.getenv("AUTOHAND_MODEL");
        String capturedError = captureRuntimeError();

        Agent agent = Agent.create(AgentOptions.builder()
            .cwd(targetRepo)
            .cliPath(cliPath)
            .model(model)
            .instructions(String.join("\n",
                "You are a QA engineering agent that turns production error reports into small repair pull requests.",
                "Reproduce the failure when the repository makes that possible.",
                "Fix the root cause, add or update a focused regression test, run the relevant validation command, commit the fix, push a branch, and create a pull request.",
                "Keep the pull request description concise and include the error signature, the fix summary, and the validation result."))
            .build());

        try {
            var run = agent.send(String.join("\n",
                "A runtime error was captured by the application error boundary.",
                "Use this error report to repair the application automatically.",
                "",
                "Captured error:",
                "```text",
                capturedError,
                "```",
                "",
                "Expected user impact:",
                "A checkout session should still calculate a safe default discount when the customer object is missing.",
                "",
                "Please create a pull request with the fix."));

            run.stream(event -> {
                switch (event) {
                    case Events.MessageUpdateEvent message -> System.out.print(message.delta());
                    case Events.ToolStartEvent tool -> System.out.println("\n[tool] " + tool.toolName());
                    case Events.PermissionRequestEvent permission -> System.out.println("\n[permission] " + permission.tool() + ": " + permission.description());
                    case Events.ErrorEvent error -> System.err.println("\n[error] " + error.message());
                    default -> {}
                }
            });

            RunResult result = run.waitForResult();
            System.out.println("\n\nRun " + result.id() + " " + result.status() + ".");
        } finally {
            agent.close();
        }
    }

    static double checkoutDiscount(Cart cart) {
        try {
            if (cart.customer().loyaltyTier().equals("gold")) {
                return cart.subtotal() * 0.15;
            }
            return cart.subtotal() * 0.05;
        } catch (RuntimeException error) {
            throw new IllegalStateException("checkout discount failed: " + error.getMessage(), error);
        }
    }

    static String captureRuntimeError() {
        try {
            checkoutDiscount(new Cart(129, null));
        } catch (Exception error) {
            return error + "\n" + stackTrace(error);
        }

        return String.join("\n",
            "IllegalStateException: checkout discount failed: Cannot invoke Customer.loyaltyTier() because customer is null",
            "    at checkout.Discounts.checkoutDiscount(Discounts.java:42)",
            "    at checkout.Session.createCheckoutSession(Session.java:88)",
            "Request: POST /checkout",
            "Payload: {\"subtotal\":129,\"customer\":null}");
    }

    static String stackTrace(Exception error) {
        java.io.StringWriter writer = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(writer));
        return writer.toString();
    }

    static String getenvOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
