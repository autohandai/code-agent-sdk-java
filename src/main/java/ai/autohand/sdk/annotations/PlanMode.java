package ai.autohand.sdk.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables plan mode for the agent (inspection only, no writes).
 *
 * <pre>{@code
 * @PlanMode
 * public class DiscoveryAgent {}
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PlanMode {
    /** Whether plan mode is enabled. */
    boolean enabled() default true;
}
