package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.helper.anno.StrWs;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Regex;

public class PegParser2BTest extends PegParserTestBase
{
    // regex group
    public record Foo(
        @Regex(value="a(b)c", group = 1)String s,
        @Regex(value="a(b|b1)c", group = 1)char c  // b1->fatal
    ){}

    {
        //generateJavaHere=true;
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        matchFull("abcabc", new Foo("b", 'b'));

        //dumpResult=true;
        matchFatal("abcab1c", 4, 3, 0);
    }
}
