package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.spec.Not;
import org.rekex.spec.Peek;
import org.rekex.spec.Permits;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.grammar.GrammarTestBase;

import java.util.List;

public class GrammarGood3Test extends GrammarTestBase
{
    @Test
    void testOverall() throws Exception
    {
        var grammar = derive(Overall.class);
        //dump(grammar);
    }

    public record Foo(@Ch("")char c){}
    public record Foo1(Foo f0){}
    public record Foo2(Foo f0, Foo1 f1){}

    @Permits(Permits1.A.class)
    public interface Permits1{
        record A(Foo foo) implements Permits1{}
    }
    @Permits({Permits2.A.class, Permits2.B.class})
    public interface Permits2{
        record A(Foo foo) implements Permits2{}
        record B(Foo foo) implements Permits2{}
    }

    public interface Ctor1{
        @Ctor public static
        Ctor1 a(){ return null; }
    }
    public static class Ctor2<T>
    {
        @Ctor(2) public
        Ctor2(T t){ }

        @Ctor(1) public static <T>
        Ctor2<T> b(T t){ return null; }
    }

    public enum EnumWithCtor{
        a,b,c;
        @Ctor public static
        EnumWithCtor ctor(@Ch("abc")String s){
            return valueOf(s);
        }
    }

    public enum Enum1{
        @Str("a") a,
    }
    public enum Enum2{
        @Str("a") a,
        @Str("b") b,
    }

    public sealed interface Sealed1{
        record A(Foo foo) implements Sealed1{}
    }
    public sealed interface Sealed2<T>{
        record A<T>(Foo foo) implements Sealed2<T>{}
        record B<T>(Foo foo) implements Sealed2<T>{}
    }

    public record Overall(
        @Ch("abc")char ch,
        @Ch("abc")Character ch2,
        @Ch(range={0,0x10FFFF}) int i,
        @Str("\uD840\uDC10") Integer i2,
        @Str("xyz")String str,
        Peek<Foo> peekFoo,
        Not<Foo> notFoo,
        Peek<Not<Foo>> notFoo2,
        List<Foo> listFoo1,
        Foo[] arrayFoo1,
        @Ch("abc")char[] arrayChar,
        Permits1 permits1,
        Permits2 permits2,
        Ctor1 ctor1,
        Ctor2<Foo> ctor2,
        EnumWithCtor ec,
        Enum1 enum1,
        Enum2 enum2,
        Sealed1 sealed1,
        Sealed2<Boolean> sealed2,
        Foo foo0,
        Foo1 foo1,
        Foo2 foo2
    ){}
}
