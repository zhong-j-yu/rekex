package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;

public class PegParserEx1Test extends PegParserTestBase
{
    public static class UserEx extends Exception
    {
        public UserEx(String msg){ super(msg); }
    }

    public record Foo(char ch)
    {
        static void check(char c1, char c2) throws IllegalArgumentException
        {
            if(c1!=c2) throw new IllegalArgumentException("not "+c2);
        }

        // throw a declared exception

        @Ctor public static Foo a(@Ch("ABCDE")char ch) throws IllegalArgumentException
        {
            check(ch, 'A');
            return new Foo(ch);
        }
        @Ctor public static Foo b(@Ch("ABCDE")char ch) throws Exception
        {
            check(ch, 'B');
            return new Foo(ch);
        }
        @Ctor public static Foo c(@Ch("ABCDE")char ch) throws IllegalArgumentException, Exception
        {
            check(ch, 'C');
            return new Foo(ch);
        }
        @Ctor public static Foo d(@Ch("ABCDE")char ch) throws IllegalArgumentException, UserEx, Error
        {
            check(ch, 'D');
            return new Foo(ch);
        }

        // throw an undeclared exception

        @Ctor public static Foo e1(@Str("E1")String s)
        {
            throw new IllegalArgumentException("E1");
        }
        @Ctor public static Foo e2(@Str("E2")String s) throws ArithmeticException
        {
            throw new IllegalArgumentException("E2");
        }
        @Ctor public static Foo e3(@Str("E3")String s) throws ArithmeticException, UserEx
        {
            throw new IllegalArgumentException("E3");
        }
    }

    {
        //generateJavaHere=true;
        //dumpJavaSource=true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("A");
        matchFull("B");
        matchFull("C");
        matchFull("D");

        matchFail("E", 0, 0, 0);

        matchFatal("E1", 0, 0, 0);
        matchFatal("E2", 0, 0, 0);
        matchFatal("E3", 0, 0, 0);

    }
}
