package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.helper.anno.AtLeast;
import org.rekex.helper.anno.AtMost;
import org.rekex.spec.Regex;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;

import java.util.List;

public class GrammarGood2Test extends GrammarTestBase
{
    public record Pair<T1,T2>(T1 t1, T2 t2){}

    public sealed interface Either<T1,T2> permits Either.Left, Either.Right
    {
        record Left<T1,T2>(T1 left) implements Either<T1,T2>{}
        record Right<T1,T2>(T2 right) implements Either<T1,T2>{}
    }

    public static abstract class Taste<T>
    {
        public static class Sweet<T> extends Taste<T>{}
        public static class Biter<T> extends Taste<T>{}

        @Ctor
        public static <T> Sweet<T> sweet(@Str("sweet")String sweet, T t){ return new Sweet<>(); }
        @Ctor
        public static <T> Biter<T> biter(@Str("biter")String biter, T t){ return new Biter<>(); }

    }

    public record Bar(
        Taste<Foo> taste,
        Foo foo,
        Pet pet,
        Baz baz,
        Pair<Pet,Pet> pair,
        Either<Pet,Baz> either
    ){
        //public Bar(){this(null,null,null);}
    }

    public record Foo(
        @AtLeast(3)@AtMost(5)List<@Regex("abc")String> s,
        @Ch("+-")char c1,
        @Ch("*") char c2
    ){}

    public enum Pet{
        @Regex("cat") cat,
        @Regex("dog") dog
    }

    public record Baz(
        Bar bar,
        List<Bar> barList,
        Bar... barArray
    ){}


    @Test
    void test() throws Exception
    {
        var grammar = derive(Bar.class);
        //dump(grammar);
    }

}
