package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;
import org.rekex.spec.Regex;

import java.io.IOException;
import java.util.Set;

public class PegParserEx2Test extends PegParserTestBase
{
    public static class SomeException extends Exception
    {
        public SomeException(String msg){ super(msg); }
    }

    public record CoolNumber(int num)
    {
        @Ctor public static
        CoolNumber fail0() throws SomeException
        {
            // if all alternatives failed, the error message is that of the 1st fail.
            // this ctor is to set up the message in case all failed.
            throw new SomeException("expecting either a prime or a square");
        }

        @Ctor public static
        CoolNumber prime(@Regex("[0-9]+")String str) throws SomeException
        {
            int num = Integer.parseInt(str);
            if(!isPrime(num))
                throw new SomeException("not a prime: " +str);
            return new CoolNumber(num);
        }
        @Ctor public static
        CoolNumber square(@Regex("[0-9]+")String str) throws SomeException
        {
            int num = Integer.parseInt(str);
            if(isSquare(num))
                return new CoolNumber(num);
            else
                throw new SomeException("not a square: " +str);
        }


        static final Set<Integer> primes = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97);
        static boolean isPrime(int num)
        {
            return primes.contains(num);
        }
        static final Set<Integer> squares = Set.of(1,4,9,16,25,36,49,64,81,100);
        static boolean isSquare(int num)
        {
            return squares.contains(num);
        }
    }

    {
        //generateJavaHere=true;
        //dumpJavaSource=true;
        initParsers(CoolNumber.class);
    }

    @Test
    void test()
    {
        //dumpResult=true;

        matchFull("5");
        matchFail("6", 0, 0, 0);
        matchFull("7");
        matchFail("8", 0, 0, 0);
        matchFull("9");

        matchFail("15", 0, 0, 0);
        matchFull("16");
        matchFull("17");
        matchFail("18", 0, 0, 0);

        matchFatal("999999999999999999999999", 0, 0, 0);

    }
}
