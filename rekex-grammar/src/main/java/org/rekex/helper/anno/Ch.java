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

import static org.rekex.regexp.RegExpApi.ch;
import static org.rekex.regexp.RegExpApi.union;


/**
 * Equivalent to a {@link Regex} that matches a single character
 * if it's in {@link #value()}  or {@link #range()} but not in {@link #except()}.
 */

@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Ch
{
    /**
     * A set of characters included.
     */
    String value() default "";
    // order does not matter.
    // enumerating a set of rather random chars is useful in a lot of grammars.
    // @Ch("xy") ~ @Str({"x","y"}); but @Ch may be more convenient.
    // @Ch("") does not match anything.

    /**
     * Ranges of characters included,
     * for example, {from, to}, {from1, to1, from2, to2}.
     */
    int[] range() default {};

    /**
     * A set of characters to be excluded.
     */
    String except() default "";

    /**
     * If true, matching is case-insensitive.
     */
    boolean ignoreCase() default false; // applies to "except" chars as well

    AnnoMacro<Ch, Regex> toRegex = thiz -> {

        RegExp.CharClass cc = ch(thiz.value()); // could be an empty union

        int[] range = thiz.range();
        if(range.length%2 != 0)
            throw new IllegalArgumentException("invalid range");
        for(int i=0; i<range.length/2; i++)
            cc = union(cc, RegExpApi.range(range[2*i], range[2*i+1]));

        if(!thiz.except().isEmpty())
            cc = cc.except(ch(thiz.except()));

        RegExp exp = cc;
        if(thiz.ignoreCase())
            exp = RegExpApi.ignoreCase(exp);
        exp = RegExpApi.simplify(exp); // flatten unions
        String regex = RegExpApi.toRegex(exp);
        return AnnoBuilder.build(Regex.class, regex);
    };
}
