package org.rekex.helper.seq;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.seq.Seq2;
import org.rekex.helper.datatype.seq.Seq3;
import org.rekex.helper.datatype.seq.Seq4;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import static org.junit.jupiter.api.Assertions.*;

public class SeqNTest extends PegParserTestBase
{
    public enum A{ @Str("A")a; }
    public enum B{ @Str("b")b; }
    public enum C{ @Str("c")c; }
    public enum D{ @Str("d")d; }

    public record Foo(
        Seq2<A,B> s2,
        Seq3<A,B,C> s3,
        Seq4<A,B,C,D> s4
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test() throws Exception
    {
        //dumpResult=true;
        matchFull("AbAbcAbcd");

        apiUsage();
    }

    void apiUsage() throws Exception
    {
        @SuppressWarnings("unchecked")
        PegParser<Foo> parser = (PegParser<Foo>)parsers.get(1);
        Foo foo = parser.matchFull("AbAbcAbcd");

        A a = foo.s2.v1();
        assertEquals(a, A.a);
        
        D d = foo.s4.v4();
        assertEquals(d, D.d);
    }
}
