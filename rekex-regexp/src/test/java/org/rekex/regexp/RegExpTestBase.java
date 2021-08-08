package org.rekex.regexp;

import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RegExpTestBase
{
    static final boolean doDump = false;
    static class Tester
    {
        private final RegExp exp;
        private final String regex;
        private final Pattern pattern;
        Tester(RegExp exp)
        {
            this.exp = exp;
            this.regex = RegExpApi.toRegex(exp);
            this.pattern = Pattern.compile(this.regex);
        }

        Tester type(Class<?> clazz)
        {
            assertTrue(clazz.isInstance(exp));
            return this;
        }

        Tester regex(String s)
        {
            assertEquals(s, regex);
            return this;
        }
        Tester dump()
        {
            Consumer<String> out = doDump ? System.out::println : s->{};
            out.accept("-----");
            out.accept(regex);
            out.accept(RegExpApi.toTreeText(exp));
            return this;
        }

        Tester matches(String... inputs)
        {
            for(var input : inputs)
                assertTrue(pattern.matcher(input).matches());
            return this;
        }
        Tester noMatch(String... inputs)
        {
            for(var input : inputs)
                assertFalse(pattern.matcher(input).matches());
            return this;
        }
        Tester lookingAt(String input, String start)
        {
            var matcher = pattern.matcher(input);
            boolean b = matcher.lookingAt();
            assertTrue(b);
            String g = matcher.group();
            assertEquals(start, g);
            return this;
        }

    }
    static Tester test(RegExp exp)
    {
        return new Tester(exp);
    }
    static Tester testSimplify(RegExp exp)
    {
        exp = RegExpApi.simplify(exp);
        return new Tester(exp );
    }

}