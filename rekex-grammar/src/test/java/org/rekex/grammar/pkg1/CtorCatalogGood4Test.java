package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.spec.Ctor;
import org.rekex.spec.Regex;

public class CtorCatalogGood4Test extends GrammarTestBase
{
    public record Foo(){}

    public sealed interface Bar{}
    public record Bar1() implements Bar{}
    public record Bar2() implements Bar{}
    public record Bar3() implements Bar{}



    public Foo foo(Bar bar1, Bar bar2)
    {
        return new Foo();
    }

    // explicit subtype ctors
    public Bar b2(Bar2 b){ return b; }
    public Bar b1(Bar1 b){ return b; }

    public Bar1 bar1(@Ch("1")char c)
    {
        return new Bar1();
    }

    public Bar2 bar2(@Ch("2")char c)
    {
        return new Bar2();
    }

    // not reachable from root
    public Bar3 bar3(@Ch("3")char c)
    {
        return new Bar3();
    }

    @Test
    void testAllSuccessClauses() throws Exception
    {
        var grammar = derive(Foo.class, CtorCatalogGood4Test.class);
        //dump(grammar);
    }

}