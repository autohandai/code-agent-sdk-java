package ai.autohand.sdk.annotations;

import ai.autohand.sdk.types.Tool;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface EnableTools {
    Tool[] value();
}
