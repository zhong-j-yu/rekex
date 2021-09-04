package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.spec.Ctor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CtorCatalogGood3Test extends GrammarTestBase
{
    // "simple" types as rule datatypes

    public double root(int i, char c, String s, double d1, Double d2, @Ch("*")char[] cc)
    {
        return 1.23;
    }

    public int i(@Ch("i")int x){ return x; }

    public char c(@Ch("c")char x){ return x; }

    public String s(@Str("str")String x){ return x; }

    public Double d2(@Ch("d")char x){ return 0.0; }

    public @Ch("*")char cc(char c1, char c2){ return c1; }
    // here @Ch("*")char is the return type of a ctor,
    // therefore it is a non-special datatype, not as a Regex rule.
    // note that it is also a subtype of `char`.

    @Test
    void testAllSuccessClauses() throws Exception
    {
        var grammar = derive(double.class, CtorCatalogGood3Test.class);
        //dump(grammar);
    }

}