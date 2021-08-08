package org.rekex.spec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The limit for the size of a List or an array:
 * `min &lt;= size &lt;= max`. Default to from 0 to Integer.MAX_VALUE.
 * See also
 * {@link org.rekex.helper.anno.AtLeast @AtLeast},
 * {@link org.rekex.helper.anno.AtMost @AtMost},
 * {@link org.rekex.helper.anno.Size @Size}.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SizeLimit
{
    /**
     * The minimum size.
     */
    int min() default 0;
    /**
     * The maximum size.
     */
    int max() default Integer.MAX_VALUE;
}
