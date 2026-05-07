package ai.autohand.sdk.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AutohandAgent {
    String model() default "";
    String instructions() default "";
}
