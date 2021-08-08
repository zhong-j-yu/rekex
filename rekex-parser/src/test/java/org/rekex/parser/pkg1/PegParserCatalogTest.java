package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.annotype.ClassType;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;

public class PegParserCatalogTest extends PegParserTestBase
{
    public sealed interface Foo{}
    public record FooA() implements Foo{}
    public record FooB() implements Foo{}

    public record Bar(Foo a, Foo b){}

    public static class CatalogClass
    {
        @Ctor public static FooA fooA(@Ch("A") char c){ return new FooA(); }
        @Ctor
        public static FooB fooB(@Ch("B") char c){ return new FooB(); }
    }

    {
        //generateJavaHere=true;
        initParsers(ClassType.of(Bar.class), CatalogClass.class);
    }

    public enum E{ A, B }

    @Test
    void test()
    {
        matchFull("AA");
        matchFull("AB");
    }
}
