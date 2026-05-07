package ai.autohand.sdk.annotations;

import ai.autohand.sdk.types.PermissionMode;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Permission {
    PermissionMode value();
}
