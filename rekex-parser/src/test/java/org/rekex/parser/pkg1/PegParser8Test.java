package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Not;
import org.rekex.spec.Peek;

public class PegParser8Test extends PegParserTestBase
{
    // Peek, Peek<Not>

    public record Foo(
        Peek<@Ch("AB")String> peek,
        Peek<Not<@Ch("BC")String>> not,
        @Ch("ABC")String str
    ){}

    {
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("A");

        matchFail("B", 0, 0,0,0);
        matchFail("C", 0, 0,0,0);
    }
}
