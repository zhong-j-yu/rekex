package org.rekex.helper;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.OneOrMore;
import org.rekex.parser.PegParserTestBase;

public class OneOrMoreTest extends PegParserTestBase
{
    public record Foo(
        OneOrMore<@Ch("a")String> aPlus
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("a");
        matchFull("aaa");

        matchPartial("a"+"b", 1);
        matchPartial("aaa"+"b", 3);

        matchFail("", 0,0,0,0,0);
        matchFail("b", 0,0,0,0,0);
    }

}
