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
import java.util.function.Function;

// Very useful to match tokens, which almost always allow spaces between.
//
// Usually user should define a macro that's convertible to this annotation,
// e.g.  @Word(val) => @StrWs(val, userWS)
//
// no @ChWs. it's not very useful. @Ch are usually not for tokens.
// but if needed, since @Ch("xy") ~ @Str({"x","y"}), @StrWs will do the job.
//

/**
 * Similar to {@link Str}, this regex will match the input to one of the string literals,
 * but it also matches following zero or more whitespace characters.
 * Only the matched string literal is the significant input (see {@link Regex#group()}.
 */
@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrWs
{
    /**
     * A list of string literals.
     */
    String[] value();

    /**
     * If true, matching is case-insensitive.
     */
    boolean ignoreCase() default false;

    /**
     * A set of characters that are considered whitespaces.
     */
    String wsChars(); // no default

    AnnoMacro<StrWs, Regex> toRegex = thiz -> {
        // e.g.  (token)[ \t]*  with group 1 captured as significant
        RegExp exp = RegExpApi.alt((Object[])thiz.value());
        if(thiz.ignoreCase())
            exp = RegExpApi.ignoreCase(exp);
        var wsChar = RegExpApi.ch(thiz.wsChars());
        var optWs = RegExpApi.rep0(wsChar);
        var g1 = RegExpApi.group(exp);
        var g0 = RegExpApi.seq(g1, optWs);
        String regex = RegExpApi.toRegex(g0);
        return AnnoBuilder.build(Regex.class,
            Regex::value, regex, Regex::group, 1);
    };

    /**
     * Utility to create an {@link AnnoMacro} that maps to {@link StrWs}
     * with a default {@link StrWs#wsChars()}.
     */
    interface Macro
    {
        static <A> AnnoMacro<A, StrWs> of(Function<A,String[]> annoToValue, String wsChars)
        {
            return of(annoToValue, false, wsChars);
        }
        static <A> AnnoMacro<A, StrWs> of(Function<A,String[]> annoToValue, boolean ignoreCase, String wsChars)
        {
            return anno-> AnnoBuilder.of(StrWs.class)
                .def(StrWs::value, annoToValue.apply(anno))
                .def(StrWs::ignoreCase, ignoreCase)
                .def(StrWs::wsChars, wsChars)
                .build();
        }
    }
}
