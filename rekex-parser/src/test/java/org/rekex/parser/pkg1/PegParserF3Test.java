package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;
import org.rekex.spec.Peek;

public class PegParserF3Test extends PegParserTestBase
{
    // Fail position

    // peek(r1 | r2)
    //   r1 failed at a further position; r2 succeeded at a shorter position
    //   peek succeeded, and it forgets fail positions of subrules.

    public record Foo(
        Peek<AB> ab,
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
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        // peek succeeded with Aa; it reached further to AaB?, but that fail info is cleared.
        // now Z failed at position 0, which is the max position
        matchFail("AaBC", 0, 0,0);
        // if peek failed, its fail position is remembered
        matchFail("A?Bb", 1, 1,0,0,0,0);
    }
}
