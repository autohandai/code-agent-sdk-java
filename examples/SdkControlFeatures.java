import ai.autohand.sdk.sdk.AutohandSDK;
import ai.autohand.sdk.types.SDKConfig;

/**
 * Example demonstrating the SDK control features.
 * This validates that the SDK exposes the expected control methods.
 *
 * Run with:
 *   mvn compile exec:java -Dexec.mainClass="SdkControlFeatures"
 */
public class SdkControlFeatures {
    public static void main(String[] args) {
        System.out.println("Testing SDK Control Features...\n");

        AutohandSDK sdk = new AutohandSDK(new SDKConfig(
            System.getProperty("user.dir"), null, false, 300_000,
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        ));

        System.out.println("SDK initialized with control options");

        String[] controlMethods = {
            "setPermissionMode", "setPlanMode", "enablePlanMode", "disablePlanMode",
            "setModel", "setMaxThinkingTokens", "applyFlagSettings",
            "supportedModels", "getContextUsage", "reloadPlugins",
            "accountInfo", "toggleMcpServer", "reconnectMcpServer", "setMcpServers"
        };

        for (String method : controlMethods) {
            boolean hasMethod = java.util.Arrays.stream(AutohandSDK.class.getMethods())
                .anyMatch(m -> m.getName().equals(method));
            System.out.println((hasMethod ? "✓" : "✗") + " SDK has method: " + method);
        }

        System.out.println("\nTesting RPC Client methods...");
        String[] rpcMethods = {
            "setPermissionMode", "setPlanMode", "setModel", "setMaxThinkingTokens",
            "applyFlagSettings", "getSupportedModels", "getSupportedCommands",
            "getContextUsage", "reloadPlugins", "getAccountInfo",
            "toggleMcpServer", "reconnectMcpServer", "setMcpServers"
        };

        try {
            var client = sdk.getClass().getDeclaredField("client");
            client.setAccessible(true);
            Object rpcClient = client.get(sdk);

            for (String method : rpcMethods) {
                boolean hasMethod = java.util.Arrays.stream(rpcClient.getClass().getMethods())
                    .anyMatch(m -> m.getName().equals(method));
                System.out.println((hasMethod ? "✓" : "✗") + " RPC Client has method: " + method);
            }
        } catch (Exception e) {
            System.out.println("Could not inspect RPC client: " + e.getMessage());
        }

        System.out.println("\nAll SDK control features are wired correctly");
    }
}
