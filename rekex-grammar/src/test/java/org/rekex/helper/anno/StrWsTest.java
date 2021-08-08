package org.rekex.helper.anno;

import org.junit.jupiter.api.Test;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.annomacro.AnnoMacro;
import org.rekex.spec.RegexTestUtil;

import java.lang.annotation.Annotation;

public class StrWsTest
{
    @Test
    void test1()
    {
        test(false, v("abc"), "abc", "abc");
        test(false, v("abc"), "abc ", "abc");
        test(false, v("abc"), "abc  ", "abc");

        test(false, v("abc"), " abc", null);
    }
    @Test
    void testIgnoreCase()
    {
        test(true, v("abc"), "ABC", "ABC");
        test(true, v("abc"), "ABC ", "ABC");
        test(true, v("abc"), " ABC", null);
    }

    static String[] v(String... arr)
    {
        return arr;
    }
    void test(boolean ignoreCase, String[] value, String input, String match)
    {
        Annotation a1 = ignoreCase
            ? AnnoBuilder.build(TokenIgnoreCase.class, value)
            : AnnoBuilder.build(Token.class, value);
        RegexTestUtil.test(a1, input, match);
    }

    final static String wsChars = " \t";
    @interface Token
    {
        String[] value();
        AnnoMacro<Token, StrWs> toStrWs = StrWs.Macro.of(Token::value, wsChars);
    }
    @interface TokenIgnoreCase
    {
        String[] value();
        AnnoMacro<TokenIgnoreCase, StrWs> toStrWs = StrWs.Macro.of(TokenIgnoreCase::value, true, wsChars);
    }
}


