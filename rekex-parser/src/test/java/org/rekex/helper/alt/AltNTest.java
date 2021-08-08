package org.rekex.helper.alt;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.alt.Alt2;
import org.rekex.helper.datatype.alt.Alt3;
import org.rekex.helper.datatype.alt.Alt4;
import org.rekex.parser.PegParserTestBase;

public class AltNTest extends PegParserTestBase
{
    public record Foo(
        Alt2<
            @Ch("a")String,
            @Ch("b")String
            > alt2,
        Alt3<
            @Ch("a")String,
            @Ch("b")String,
            @Ch("c")String
            > alt3,
        Alt4<
            @Ch("a")String,
            @Ch("b")String,
            @Ch("c")String,
            @Ch("d")String
            > alt4
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("abc");
    }
}
