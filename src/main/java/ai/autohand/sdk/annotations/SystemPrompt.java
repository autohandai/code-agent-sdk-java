package ai.autohand.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets or appends a system prompt for the agent.
 *
 * <pre>{@code
 * @SystemPrompt(value = "You are a release review agent.", mode = SystemPrompt.Mode.REPLACE)
 * public class ReviewAgent {}
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SystemPrompt {
    /** The system prompt text or file path. */
    String value();

    /** Whether to replace or append the system prompt. */
    Mode mode() default Mode.APPEND;

    enum Mode {
        /** Replace the default system prompt entirely. */
        REPLACE,
        /** Append to the default system prompt. */
        APPEND
    }
}
