package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;

import java.util.List;

public class PegParserF2Test extends PegParserTestBase
{
    // Fail position

    // repeat(AaBb) => [a0, a1, ...]
    //   a0 succeeds, but has a fail position beyond
    //   a1 fails, at a smaller position

    public record Foo(
        @Ch("[")char pL,
        List<AB> list,
        @Ch("]")char pR
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
        matchFull("[]");
        matchFull("[AaBb]");
        matchFull("[AaAa]");
        matchFull("[AaBbAa]");
        matchFull("[AaAaBb]");

        matchFail("[AaBc]", 4, 4,1,1,1,0);
    }
}
