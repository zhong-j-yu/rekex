package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;
import org.rekex.spec.Not;

public class PegParserF1Test extends PegParserTestBase
{
    // Fail position

    // concat(r1, r2),
    //   r1 succeeds, but has a fail position beyond
    //   r2 fails, at a smaller position

    public record Foo(
        AB ab,
        @Ch("Z")char z
    ){}

    // AaBb or Aa
    public static class AB
    {
        @Ctor public static AB ctor1(@Ch("A")char c1, @Ch("a")char c2, @Ch("B")char c3, @Ch("b")char c4)
        {
            return new AB();
        }
        @Ctor public static AB ctor2(@Ch("A")char c1, @Ch("a")char c2)
        {
            return new AB();
        }
    }

    {
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        matchFail("AaBC", 3, 3,0,0,0);
    }
}
