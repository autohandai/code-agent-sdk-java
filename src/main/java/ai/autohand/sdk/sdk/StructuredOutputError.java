package ai.autohand.sdk.sdk;

public final class StructuredOutputError extends Exception {
    private final String rawResponse;

    public StructuredOutputError(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse;
    }

    public String rawResponse() {
        return rawResponse;
    }

    public String getRawResponsePreview() {
        if (rawResponse == null) {
            return "";
        }
        return rawResponse.length() <= 500 ? rawResponse : rawResponse.substring(0, 500);
    }
}
