package org.rekex.helper.anno;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.regexp.RegExp;
import org.rekex.regexp.RegExpApi;
import org.rekex.spec.Regex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// match one of literal strings in value()
// mostly for defining the literal string of a token.
// enumerating a set of tokens is useful in a lot of grammars.
//
// usually, user should use StrWs, or rather a self defined equivalence of,
// to match tokens, so that trailing spaces can be skipped.
//
// Str(value={}) does not match anything.
// Str("") matches "", i.e. it succeeds in any position in an input.
//

/**
 * Equivalent to a {@link Regex} that matches a string
 * to one of the literal strings in {@link #value()}.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Str
{
    /**
     * A list of string literals.
     */
    String[] value();
    // order matters

    /**
     * If true, matching is case-insensitive.
     */
    boolean ignoreCase() default false;

    AnnoMacro<Str, Regex> toRegex = thiz -> {
        RegExp exp = RegExpApi.alt((Object[])thiz.value());
        if(thiz.ignoreCase())
            exp = RegExpApi.ignoreCase(exp);
        String regex = RegExpApi.toRegex(exp);
        return AnnoBuilder.build(Regex.class, regex);
    };
}
