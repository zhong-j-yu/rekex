package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;
import org.rekex.spec.Not;
import org.rekex.spec.Peek;

public class PegParserF4Test extends PegParserTestBase
{
    // Fail position

    // Not(r)
    //   if r fails, Not(r) succeeds; fail positions of r are discarded.
    //   if r succeeds, Not(r) fails at the spot; fail positions of r are discarded.
    // in either case, fail info of subrule are discarded.

    public record Foo(
        Not<AB> ab,
        @Ch(range={0,0xFF})char[] ch
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
        matchFail("AaBbCCC", 0, 0,0);
        matchFail("AaCCC", 0, 0,0);

        matchFull("A?CCC");
        matchFull("??CCC");
    }
}
