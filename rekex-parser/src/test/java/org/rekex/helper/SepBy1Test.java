package org.rekex.helper;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SepBy1Test extends PegParserTestBase
{
    public enum A{ @Str("a")a; }
    public enum Comma{ @Str(",")comma; }

    public record Foo(
        SepBy1<A, Comma> as
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test() throws Exception
    {
        //dumpResult=true;
        matchFull("a");
        matchFull("a,a");
        matchFull("a,a,a");

        matchPartial("a,", 1);
        matchPartial("a,a,", 3);

        matchFail("", 0,0,0,0);
        matchFail(",a", 0,0,0,0);

        apiUsage();
    }

    void apiUsage() throws Exception
    {
        @SuppressWarnings("unchecked")
        PegParser<Foo> parser = (PegParser<Foo>)parsers.get(1);
        Foo foo = parser.matchFull("a,a");
        assertEquals(foo.as.values(), List.of(A.a, A.a));
        assertEquals(foo.as.separators(), List.of(Comma.comma));
    }
}
