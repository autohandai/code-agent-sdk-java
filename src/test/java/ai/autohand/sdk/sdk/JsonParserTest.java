package ai.autohand.sdk.sdk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonParserTest {

    record Simple(String name, int value) {}

    @Test
    void parseDirectJson() throws StructuredOutputError {
        String json = "{\"name\":\"test\",\"value\":42}";
        Simple result = JsonParser.parseJsonText(json, Simple.class);
        assertEquals("test", result.name());
        assertEquals(42, result.value());
    }

    @Test
    void parseFencedJson() throws StructuredOutputError {
        String text = "Some text\n```json\n{\"name\":\"fenced\",\"value\":1}\n```\nMore text";
        Simple result = JsonParser.parseJsonText(text, Simple.class);
        assertEquals("fenced", result.name());
        assertEquals(1, result.value());
    }

    @Test
    void parseEmbeddedJson() throws StructuredOutputError {
        String text = "Here is the data: {\"name\":\"embedded\",\"value\":2} and more text";
        Simple result = JsonParser.parseJsonText(text, Simple.class);
        assertEquals("embedded", result.name());
        assertEquals(2, result.value());
    }

    @Test
    void parseEmptyThrows() {
        assertThrows(StructuredOutputError.class, () ->
            JsonParser.parseJsonText("", Simple.class)
        );
    }

    @Test
    void parseInvalidThrows() {
        assertThrows(StructuredOutputError.class, () ->
            JsonParser.parseJsonText("not json at all", Simple.class)
        );
    }

    @Test
    void buildJsonInstructionContainsRules() {
        String instruction = JsonParser.buildJsonInstruction("MySchema", Map.of("key", "string"), "Be precise");
        assertTrue(instruction.contains("Return only valid JSON"));
        assertTrue(instruction.contains("MySchema"));
        assertTrue(instruction.contains("Be precise"));
    }
}
