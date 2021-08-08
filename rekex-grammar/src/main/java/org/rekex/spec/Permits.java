package org.rekex.spec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Similar to the permits clause of a sealed type,
 * with a well defined order among subtypes.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Permits
{
    /**
     * Subtypes permitted. The order is significant.
     */
    Class<?>[] value();
}
