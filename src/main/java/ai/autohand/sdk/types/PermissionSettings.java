package ai.autohand.sdk.types;

import java.util.List;

public record PermissionSettings(
        String mode,
        List<PermissionRule> allowRules,
        List<PermissionRule> denyRules,
        List<String> allowPatterns,
        List<String> denyPatterns
) {
}
