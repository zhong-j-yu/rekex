package org.rekex.helper.alt;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.alt.Alt2;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class Alt2Test extends PegParserTestBase
{
    public record Foo(
        Alt2<
            @Ch("a")String,
            @Ch("b")String
            > alt
    ){}

    {
        //generateJavaHere=true;
        initParsers(Foo.class, null);
    }

    @Test
    void test() throws Exception
    {
        //dumpResult=true;
        matchFull("a");
        matchFull("b");

        matchFail("Z", 0, 0,0,0,0);

        apiUsage();
    }

    void apiUsage() throws Exception
    {
        @SuppressWarnings("unchecked")
        PegParser<Foo> parser = (PegParser<Foo>)parsers.get(1);
        Foo foo = parser.matchFull("a");
        if(foo.alt instanceof Alt2.Choice1<String,?> c1)
        {
            String s1 = c1.value();
            assertEquals(s1, "a");
        }
        else
            fail();
    }
}
