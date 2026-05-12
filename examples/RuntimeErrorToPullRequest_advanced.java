import ai.autohand.sdk.sdk.Agent;
import ai.autohand.sdk.sdk.AgentOptions;
import ai.autohand.sdk.sdk.RunResult;
import ai.autohand.sdk.types.Events;

import java.util.List;
import java.util.Map;

/**
 * Advanced runtime error to pull request.
 *
 * This example builds a production incident packet, confirms that GitHub
 * credentials are available through the environment, and asks Autohand to
 * reproduce the failure, patch the app, run validation, commit, push, and open
 * a pull request.
 *
 * Required:
 *   AUTOHAND_TARGET_REPO=/path/to/app
 *   GITHUB_TOKEN or GH_TOKEN with repo scope
 */
public class RuntimeErrorToPullRequest_advanced {
    record GitHubCredentials(String tokenEnvName, String remote, String baseBranch, String repository) {}

    record IncidentPacket(
        String id,
        String severity,
        String service,
        String firstSeen,
        String release,
        String errorSignature,
        String userImpact,
        String stackTrace,
        List<String> logs,
        Map<String, Object> request,
        List<String> suspectedFiles,
        String reproductionCommand,
        List<String> validationCommands
    ) {}

    public static void main(String[] args) throws Exception {
        String targetRepo = getenvOrDefault("AUTOHAND_TARGET_REPO", ".");
        GitHubCredentials github = githubCredentialsFromEnv();
        IncidentPacket incident = captureIncidentPacket();

        Agent agent = Agent.create(AgentOptions.builder()
            .cwd(targetRepo)
            .cliPath(System.getenv("AUTOHAND_CLI_PATH"))
            .model(System.getenv("AUTOHAND_MODEL"))
            .instructions("Work like a careful senior QA engineer. Keep secrets out of logs and pull request text.")
            .build());

        try {
            var run = agent.send(buildPrompt(incident, github));
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

    static GitHubCredentials githubCredentialsFromEnv() {
        String tokenEnvName;
        if (hasEnv("GITHUB_TOKEN")) {
            tokenEnvName = "GITHUB_TOKEN";
        } else if (hasEnv("GH_TOKEN")) {
            tokenEnvName = "GH_TOKEN";
        } else {
            throw new IllegalStateException("Set GITHUB_TOKEN or GH_TOKEN before running this example.");
        }

        return new GitHubCredentials(
            tokenEnvName,
            getenvOrDefault("AUTOHAND_GITHUB_REMOTE", "origin"),
            getenvOrDefault("AUTOHAND_GITHUB_BASE_BRANCH", "main"),
            System.getenv("GITHUB_REPOSITORY")
        );
    }

    static IncidentPacket captureIncidentPacket() {
        return new IncidentPacket(
            "INC-2026-05-12-0417",
            "sev2",
            "checkout-api",
            "2026-05-12T09:14:22Z",
            "checkout-api@2026.05.12.3",
            "IllegalStateException: checkout discount failed while replaying coupon idempotency key",
            "Checkout returns HTTP 500 for guest customers using coupon replay from mobile clients.",
            String.join("\n", List.of(
                "IllegalStateException: checkout discount failed while replaying coupon idempotency key",
                "    at checkout.Discounts.calculateDiscount(Discounts.java:42)",
                "    at checkout.PaymentIntent.buildPaymentIntent(PaymentIntent.java:118)",
                "    at checkout.Session.createCheckoutSession(Session.java:88)"
            )),
            List.of(
                "level=error trace=trk_94 request_id=req_7f2 route=POST /checkout status=500 duration_ms=184",
                "level=warn trace=trk_94 idempotency_key=checkout:cart_live_9834:attempt_2 cache_status=miss",
                "level=info trace=trk_94 feature_flags=discount-v2,coupon-replay"
            ),
            Map.of(
                "method", "POST",
                "path", "/checkout",
                "payload", Map.of(
                    "cartId", "cart_live_9834",
                    "subtotal", 129,
                    "customer", "null",
                    "coupon", Map.of("code", "SPRING25", "source", "mobile-v5"),
                    "idempotencyKey", "checkout:cart_live_9834:attempt_2"
                ),
                "headers", Map.of("x-client-version", "ios/5.18.0", "x-request-id", "req_7f2")
            ),
            List.of(
                "src/main/java/checkout/Discounts.java",
                "src/main/java/checkout/PaymentIntent.java",
                "src/main/java/checkout/Session.java",
                "src/test/java/checkout/SessionTest.java"
            ),
            "mvn -q -Dtest=SessionTest#guestCouponReplay test",
            List.of(
                "mvn -q -Dtest=SessionTest#guestCouponReplay test",
                "mvn test",
                "mvn package"
            )
        );
    }

    static String buildPrompt(IncidentPacket incident, GitHubCredentials github) {
        String repoHint = github.repository() == null || github.repository().isBlank()
            ? "- Discover the GitHub repository from git remote output."
            : "- GitHub repository hint: " + github.repository() + ".";

        return String.join("\n",
            "You are a senior QA engineering agent responsible for converting production incidents into verified repair pull requests.",
            "",
            "GitHub credentials:",
            "- A GitHub token is available in the " + github.tokenEnvName() + " environment variable. Do not print or commit the token.",
            "- Use git remote " + github.remote() + ".",
            "- Open the pull request against " + github.baseBranch() + ".",
            repoHint,
            "- Before pushing, run gh auth status or an equivalent non-secret auth check.",
            "",
            "Incident packet:",
            "```text",
            incident.toString(),
            "```",
            "",
            "Required workflow:",
            "1. Inspect the target repository and confirm the likely failing path.",
            "2. Reproduce the incident using the provided payload or nearest existing test harness.",
            "3. Fix the root cause, not just the thrown exception.",
            "4. Add a regression test covering guest checkout, coupon replay, and idempotency behavior.",
            "5. Run the focused test first, then the relevant validation commands.",
            "6. Create a branch named autohand/fix-checkout-incident-inc-2026-05-12-0417.",
            "7. Commit the fix with a clear message.",
            "8. Push the branch and open a pull request.",
            "9. In the PR body, include the incident id, error signature, files changed, tests run, and any residual risk."
        );
    }

    static boolean hasEnv(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }

    static String getenvOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
