package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.Ch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CtorCatalogGood2Test extends GrammarTestBase
{
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface N{ String value(); }

    public record Foo(@N("foo")int foo){}

    @Ctor public static
    @N("foo")int foo(@N("bar")char bar)
    {
        return bar+bar;
    }

    @Ctor public static
    @N("bar")char bar(@Ch("bar")char ch)
    {
        return ch;
    }

    @Test
    void testAllSuccessClauses() throws Exception
    {
        var grammar = derive(Foo.class, CtorCatalogGood2Test.class);
        //dump(grammar);
    }

}