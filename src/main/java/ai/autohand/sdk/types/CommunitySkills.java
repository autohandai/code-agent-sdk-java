package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** Typed community skill-registry and installation payloads. */
public final class CommunitySkills {
    private CommunitySkills() {
    }

    public record RegistryParams(@JsonInclude(JsonInclude.Include.NON_NULL) Boolean forceRefresh) {
        public static RegistryParams cached() {
            return new RegistryParams(null);
        }
    }

    public record Skill(
            String id,
            String name,
            String description,
            String category,
            List<String> tags,
            Double rating,
            Integer downloadCount,
            Boolean isFeatured,
            Boolean isCurated) {
    }

    public record Category(String name, int count) {
    }

    public record RegistryResult(boolean success, List<Skill> skills, List<Category> categories, String error) {
    }

    public enum Scope {
        USER("user"),
        PROJECT("project");

        private final String cliValue;

        Scope(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }
    }

    public record InstallParams(
            String skillName,
            Scope scope,
            @JsonInclude(JsonInclude.Include.NON_NULL) Boolean force) {
        public InstallParams {
            if (skillName == null || skillName.isBlank()) {
                throw new IllegalArgumentException("A non-empty skill name is required.");
            }
            if (scope == null) {
                throw new IllegalArgumentException("A skill installation scope is required.");
            }
        }

        public InstallParams(String skillName, Scope scope) {
            this(skillName, scope, null);
        }
    }

    public record InstallResult(boolean success, String skillName, String path, String error) {
    }
}
