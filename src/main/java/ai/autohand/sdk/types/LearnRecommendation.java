package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** Typed project-learning audit and skill recommendations. */
public final class LearnRecommendation {
    private LearnRecommendation() {
    }

    public record Params(@JsonInclude(JsonInclude.Include.NON_NULL) Boolean deep) {
        public static Params standard() {
            return new Params(null);
        }
    }

    public enum AuditStatus {
        REDUNDANT("redundant"),
        OUTDATED("outdated"),
        CONFLICTING("conflicting");

        private final String cliValue;

        AuditStatus(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }

        @JsonCreator
        public static AuditStatus fromCliValue(String value) {
            for (AuditStatus status : values()) {
                if (status.cliValue.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown learning audit status: " + value);
        }
    }

    public record Audit(String skill, AuditStatus status, String reason) {
    }

    public record Recommendation(String slug, double score, String reason) {
    }

    public record Result(
            boolean success,
            String projectSummary,
            List<Audit> audit,
            List<Recommendation> recommendations,
            String gapAnalysis,
            String error) {
        public Result {
            audit = audit == null ? List.of() : List.copyOf(audit);
            recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        }
    }
}
