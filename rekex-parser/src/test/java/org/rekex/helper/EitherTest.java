package org.rekex.helper;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.Either;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class EitherTest extends PegParserTestBase
{
    public enum A{ @Str("a")a; }
    public enum B{ @Str("b")b; }
    public enum C{ @Str("c")c; }
    public enum D{ @Str("d")d; }

    public record Foo(
        Either<A,B> e1,
        Either<C,D> e2
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test() throws Exception
    {
        //dumpResult=true;
        matchFull("ad");

        apiUsage();
    }

    void apiUsage() throws Exception
    {
        @SuppressWarnings("unchecked")
        PegParser<Foo> parser = (PegParser<Foo>)parsers.get(1);
        Foo foo = parser.matchFull("ad");

        if(foo.e1 instanceof Either.Left<A,?> c1)
        {
            A a = c1.value();
            assertEquals(a, A.a);
        }
        else
            fail();

        String s1 = foo.e1.caseOf(A->"A", B->"B");
        assertEquals(s1, "A");
    }
}
