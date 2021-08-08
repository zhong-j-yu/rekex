package org.rekex.spec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for constructors and methods to mark them as a ctor.
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Ctor
{
    /**
     * The ordinal of this ctor among related ctors;
     * recommended to be "1, 2, 3, ...".
     */
    int value() default ORDER_UNSPECIFIED;

    int ORDER_UNSPECIFIED = -1;
}
