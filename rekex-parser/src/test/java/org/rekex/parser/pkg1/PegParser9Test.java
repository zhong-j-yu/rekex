package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Not;

public class PegParser9Test extends PegParserTestBase
{
    // Not<Not>, Not

    public record Foo(
        Not<Not<@Ch("AB")String>> peek,
        Not<@Ch("BC")String> not,
        @Ch("ABC")String str
    ){}

    {
        //generateJavaHere=true;
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("A");

        matchFail("B", 0, 0,0);
        matchFail("C", 0, 0,0);
    }
}
