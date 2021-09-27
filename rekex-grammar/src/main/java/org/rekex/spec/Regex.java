package org.rekex.spec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The regular expression that the value of a string-like dataype must match.
 */
@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Regex
{
    // should be as greedy as possible
    // e.g. "ab|abc" is problematic; a parser may
    // use it to match "ab" in the of "abc...", the proceeds to
    // match "c..." with another rule; when that fails,
    // the parser doesn't backtrace and try this regex again
    // (and if it does, this regex will give the same result)

    /**
     * The regular expression, as defined in {@link java.util.regex.Pattern}.
     */
    String value();

    /**
     * The flags, as defined in {@link java.util.regex.Pattern}.
     */
    int flags() default 0;

    /**
     * The group number of the group to be captured,
     * as the significant payload. 
     */
    int group() default 0;
    // the rest are matched and consumed, but ignored.

}
