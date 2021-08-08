package org.rekex.helper;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.Opt;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class OptTest extends PegParserTestBase
{
    public enum A{ @Str("a")a; }
    public enum B{ @Str("b")b; }
    public enum C{ @Str("c")c; }
    public enum D{ @Str("d")d; }

    public record Foo(
        A a, Opt<B> b, C c, Opt<D> d
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test() throws Exception
    {
        //dumpResult=true;
        matchFull("abcd");
        matchFull("ac");

        apiUsage();
    }

    void apiUsage() throws Exception
    {
        @SuppressWarnings("unchecked")
        PegParser<Foo> parser = (PegParser<Foo>)parsers.get(1);
        Foo foo = parser.matchFull("abc");

        if(foo.b instanceof Opt.Some<B> c1)
        {
            B b = c1.value();
            assertEquals(b, B.b);
        }
        else
            fail();

        String sb = foo.b.caseOf(some->"B", ()->"");
        assertEquals(sb, "B");

        String sd = foo.d.caseOf(some->"D", ()->"");
        assertEquals(sd, "");
    }
}
