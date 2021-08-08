package org.rekex.helper.alt;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.alt.Alt3;
import org.rekex.parser.PegParserTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class Alt3Test extends PegParserTestBase
{
    public record Foo(
        Alt3<
            @Ch("a")String,
            @Ch("b")String,
            @Ch("c")String
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

        matchFail("Z", 0, 0,0,0,0);

    }
}
