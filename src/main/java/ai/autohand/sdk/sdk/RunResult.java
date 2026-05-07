package ai.autohand.sdk.sdk;

import ai.autohand.sdk.types.Event;
import java.util.List;

public record RunResult(String id, String status, String text, List<Event> events) {
}
