package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.annotype.ClassType;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;

public class PegParserCatalog2Test extends PegParserTestBase
{
    public sealed interface Foo{}
    public record FooA(boolean b) implements Foo{}
    public record FooB(boolean b) implements Foo{}

    public record Bar(Foo a, Foo b){}

    public record CatalogClass(boolean config)
    {
        public
        FooA fooA(@Ch("A") char c){ return new FooA(config); }

        public static
        FooB fooB(@Ch("B") char c){ return new FooB(false); }
    }

    {
        //generateJavaHere=true;
        initParsers(ClassType.of(Bar.class), CatalogClass.class, new CatalogClass(true));
    }

    public enum E{ A, B }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("AA");
        matchFull("AB");
    }
}
