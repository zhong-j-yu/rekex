package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.spec.Regex;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.Str;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CtorCatalogGood1Test extends GrammarTestBase
{
    public record Foo(){}
    public interface Bar{}
    public record Bar1() implements Bar{}
    public record Bar2() implements Bar{}
    public record Boxed<T>(T v){}

    @Ctor public static
    Foo foo(Bar bar1, Bar bar2, Boxed<@A()Integer> box1, Boxed<Bar> box2)
    {
        return new Foo();
    }

    @Ctor public static
    Bar1 bar(@A()Integer i)
    {
        return new Bar1();
    }

    @Ctor public static
    Bar2 bar(@A()Boolean b)
    {
        return new Bar2();
    }

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    public @interface A{}

    @Ctor public static
    @A()Integer num(@Regex("\\d+") String s)
    {
        return Integer.parseInt(s);
    }

    @Ctor public static
    @A()Boolean bool(@Str({"true","false"}) String s)
    {
        return Boolean.parseBoolean(s);
    }

    @Ctor public static
    <T> Boxed<T> boxed(@Str("boxed")String s, T v)
    {
        return new Boxed<>(v);
    }

    @Test
    void testAllSuccessClauses() throws Exception
    {
        var grammar = derive(Foo.class, CtorCatalogGood1Test.class);
        //dump(grammar);
    }

}