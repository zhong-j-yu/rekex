package org.rekex.helper;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.Epsilon;
import org.rekex.helper.datatype.alt.Alt2;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EpsilonTest extends PegParserTestBase
{
    public record Foo(
        Alt2<@Ch("a")String, Epsilon> optA,
        @Ch("b")String b,
        Epsilon e,
        @Ch("c")String c
    ){}

    {
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("abc");
        matchFull("bc");
    }

}
