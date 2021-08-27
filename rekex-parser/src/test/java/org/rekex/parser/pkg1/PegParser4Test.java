package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.PegParserTestBase;

public class PegParser4Test extends PegParserTestBase
{
    // concat
    public record Foo(
        Pair<XY,Pair<XY, XY>> arg0,
        AB<XY> arg1,
        Epsilon epsilon
    ){}

    public enum XY
    {
        @Str("x")x,
        @Str("y")y,
    }

    public record Epsilon()
    {
        @Ctor public Epsilon{}
    }

    public record Pair<T1,T2>(T1 v1, T2 v2){}

    public static class AB<T>
    {
        public static class A<T> extends AB<T>
        {}
        public static class B<T> extends AB<T>
        {}

        @Ctor public static <T> A<T> ctorA(@Ch("A")char a, T t)
        {
            if(t==XY.x)
                throw new IllegalArgumentException("invalid combination Ax");
            return new A<>();
        }
        @Ctor public static <T> B<T> ctorB(@Ch("B")char b, T t) throws Exception
        {
            throw new Exception("fatal error ctorB");
        }
    }

    {
        //generateJavaHere=true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        matchFull("xyxAy");

        matchFail("xy?Ax", 2, 2, 2, 1, 0, 0);

        matchFail("xyxA?", 4, 4,4,3,3,0);

        matchFail("xyx?x", 3, 3,3,3,0);

        matchFail("xyxAx", 5, 3,3,0); // Ax matched, ruled out by ctorA

        //dumpResult=true;
        matchFatal("xyxBx??", 5, 3,3,0); // Bx matched, fatal error by ctorB
    }
}
