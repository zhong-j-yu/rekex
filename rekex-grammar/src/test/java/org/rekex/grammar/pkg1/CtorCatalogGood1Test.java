package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.spec.Regex;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.Str;

public class CtorCatalogGood1Test extends GrammarTestBase
{
    public record Foo(){}
    public sealed interface Bar{}
    public record Bar1() implements Bar{}
    public record Bar2() implements Bar{}
    public record Boxed<T>(T v){}

    public
    Foo foo(Bar bar1, Bar bar2, Boxed<Integer> box1, Boxed<Bar> box2)
    {
        return new Foo();
    }

    @Ctor public
    Bar1 bar(Integer i)
    {
        return new Bar1();
    }

    public static
    Bar2 bar(Boolean b)
    {
        return new Bar2();
    }

    @Ctor public static
    <T> Boxed<T> boxed(@Str("boxed")String s, T v)
    {
        return new Boxed<>(v);
    }

    public
    Integer num(@Regex("\\d+") String s)
    {
        return Integer.parseInt(s);
    }

    public
    Boolean bool(@Str({"true","false"}) String s)
    {
        return Boolean.parseBoolean(s);
    }

    @Test
    void testAllSuccessClauses() throws Exception
    {
        var grammar = derive(Foo.class, CtorCatalogGood1Test.class);
        //dump(grammar);
    }

}