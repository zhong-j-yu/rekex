package org.rekex.helper.alt;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.alt.Alt4;
import org.rekex.parser.PegParserTestBase;

public class Alt4Test extends PegParserTestBase
{
    public record Foo(
        Alt4<
            @Ch("a")String,
            @Ch("b")String,
            @Ch("c")String,
            @Ch("d")String
            > alt
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("a");
        matchFull("b");
        matchFull("c");
        matchFull("d");

        matchFail("Z", 0, 0,0,0,0);

    }
}
