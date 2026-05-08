package ai.autohand.sdk.sdk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON parsing utilities for structured agent output.
 *
 * <p>Provides methods to build JSON instruction prompts and extract valid JSON
 * from agent responses that may be wrapped in Markdown fences or contain
 * embedded JSON objects.</p>
 *
 * @see StructuredOutputError
 */
public final class JsonParser {
    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonParser() {}

    /**
     * Builds a JSON instruction string to append to prompts requesting structured output.
     *
     * @param schemaName the expected schema type name, or {@code null}
     * @param schema the schema description (Map, List, or JSON string), or {@code null}
     * @param outputInstructions additional output instructions, or {@code null}
     * @return the formatted instruction string
     */
    public static String buildJsonInstruction(String schemaName, Object schema, String outputInstructions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Return only valid JSON.\n");
        sb.append("Do not wrap the response in Markdown.\n");
        sb.append("Do not include commentary outside the JSON value.\n");
        if (schemaName != null && !schemaName.isEmpty()) {
            sb.append("The JSON value should satisfy: ").append(schemaName).append(".\n");
        }
        if (schema != null) {
            try {
                sb.append("Use this JSON schema or example shape:\n")
                    .append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema)).append("\n");
            } catch (JsonProcessingException ignored) {}
        }
        if (outputInstructions != null && !outputInstructions.isEmpty()) {
            sb.append(outputInstructions).append("\n");
        }
        return sb.toString();
    }

    /**
     * Parses JSON from agent response text, trying multiple extraction strategies.
     *
     * <p>Attempts in order: direct parse, fenced JSON blocks (```{@code ```json}),
     * and embedded JSON substrings.</p>
     *
     * @param <T> the target type
     * @param text the raw agent response text
     * @param clazz the class to deserialize into
     * @return the parsed object
     * @throws StructuredOutputError if no valid JSON can be extracted
     */
    public static <T> T parseJsonText(String text, Class<T> clazz) throws StructuredOutputError {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            throw new StructuredOutputError("Expected JSON output, received an empty response.", text);
        }
        // Try direct parse
        try {
            return mapper.readValue(trimmed, clazz);
        } catch (JsonProcessingException ignored) {}

        // Try fenced JSON
        Object fenced = parseFencedJson(trimmed, clazz);
        if (fenced != null) return clazz.cast(fenced);

        // Try embedded JSON
        Object embedded = parseEmbeddedJson(trimmed, clazz);
        if (embedded != null) return clazz.cast(embedded);

        throw new StructuredOutputError("Expected valid JSON output from the agent.", text);
    }

    private static <T> T parseFencedJson(String text, Class<T> clazz) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1).trim();
            if (candidate.isEmpty()) continue;
            try {
                return mapper.readValue(candidate, clazz);
            } catch (JsonProcessingException ignored) {}
        }
        return null;
    }

    private static <T> T parseEmbeddedJson(String text, Class<T> clazz) {
        List<String> candidates = findJsonSubstrings(text);
        for (String candidate : candidates) {
            try {
                return mapper.readValue(candidate, clazz);
            } catch (JsonProcessingException ignored) {}
        }
        return null;
    }

    private static List<String> findJsonSubstrings(String text) {
        List<String> candidates = new ArrayList<>();
        List<Character> stack = new ArrayList<>();
        int startIndex = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{' || c == '[') {
                if (stack.isEmpty()) startIndex = i;
                stack.add(c);
                continue;
            }
            if ((c == '}' || c == ']') && !stack.isEmpty()) {
                char opener = stack.getLast();
                boolean matches = (opener == '{' && c == '}') || (opener == '[' && c == ']');
                if (!matches) {
                    stack.clear();
                    startIndex = -1;
                    continue;
                }
                stack.removeLast();
                if (stack.isEmpty() && startIndex >= 0) {
                    candidates.add(text.substring(startIndex, i + 1));
                    startIndex = -1;
                }
            }
        }
        return candidates;
    }
}
