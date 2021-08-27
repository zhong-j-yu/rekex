package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Regex;
import org.rekex.parser.PegParserTestBase;

public class PegParser3Test extends PegParserTestBase
{
    public record Foo(
        @Regex(value="(xy|a)|b", group=1)
        char c1
    ){}

    {
        //generateJavaHere=true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        matchFull("a");

        //dumpResult=true;
        matchFail("b", 0, 0, 0); // regex matches, but not the group

        //dumpResult=true;
        matchFatal("xy", 0, 0, 0);  // regex matches, but not for a single char

    }
}
