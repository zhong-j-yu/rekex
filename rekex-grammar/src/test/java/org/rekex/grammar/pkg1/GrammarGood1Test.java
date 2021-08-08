package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.spec.SizeLimit;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.*;

import java.util.List;

public class GrammarGood1Test extends GrammarTestBase
{
    public record Recur1(Recur2 r2){}
    public record Recur2(Recur1 r2){}

    @Test
    void testRecur() throws Exception
    {
        var grammar = derive(Recur1.class);
        //dump(grammar);
    }



    public static class Catalog1
    {
        @Ctor
        public static
        Recur1 recur1(@Str("abc")String str, Recur2 recur2){
            return new Recur1(recur2);
        }
    }
    @Test
    void testCatalog1() throws Exception
    {
        var grammar = derive(Recur1.class, Catalog1.class);
        //dump(grammar);
    }



    public record Rep(
        List<Rep> listFoo,
        @SizeLimit(min=2,max=3) @AtLeast(1)@AtMost(4) List<Rep> listFoo2,
        Rep[] arrFoo,
        Rep @Size(4)[] arrFoo2,
        @Ch("abc")char[] arrayChar,
        @Ch("abc")char@Size(4)[] arrayChar2
    ){}
    @Test
    void testRepeat() throws Exception
    {
        var grammar = derive(Rep.class);
        //dump(grammar);
    }

}
