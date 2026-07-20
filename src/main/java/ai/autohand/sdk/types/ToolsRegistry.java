package ai.autohand.sdk.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

/** Typed snapshot of tools registered with the CLI and registry diagnostics. */
public final class ToolsRegistry {
    private ToolsRegistry() {
    }

    public enum Source {
        BUILTIN("builtin"), META("meta"), EXTENSION("extension");

        private final String cliValue;

        Source(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }

        @JsonCreator
        public static Source fromCliValue(String value) {
            for (Source source : values()) {
                if (source.cliValue.equals(value)) return source;
            }
            throw new IllegalArgumentException("Unknown tool source: " + value);
        }
    }

    public enum Scope {
        USER("user"), PROJECT("project");

        private final String cliValue;

        Scope(String cliValue) {
            this.cliValue = cliValue;
        }

        @JsonValue
        public String cliValue() {
            return cliValue;
        }

        @JsonCreator
        public static Scope fromCliValue(String value) {
            for (Scope scope : values()) {
                if (scope.cliValue.equals(value)) return scope;
            }
            throw new IllegalArgumentException("Unknown tool scope: " + value);
        }
    }

    public record Tool(
            String name,
            String description,
            Boolean requiresApproval,
            String approvalMessage,
            Source source,
            Scope scope,
            Boolean disabled,
            String createdAt,
            Integer schemaVersion,
            String handlerPreview,
            String reuseHint,
            String extensionId,
            String extensionVersion) {
    }

    public record Diagnostic(String file, String reason) {
    }

    public record Result(List<Tool> tools, List<Diagnostic> diagnostics) {
        public Result {
            tools = tools == null ? List.of() : List.copyOf(tools);
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }
}
