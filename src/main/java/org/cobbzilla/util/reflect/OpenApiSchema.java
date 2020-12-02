package org.cobbzilla.util.reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface OpenApiSchema {

    String[] exclude() default "";
    String[] include() default "";

}
