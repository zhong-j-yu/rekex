package org.rekex.exmple.parser.calculator;

import org.junit.jupiter.api.Test;
import org.rekex.parser.PegParser;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ExampleParser_CalculatorsTest
{
    record Tester<T>(boolean ws, PegParser<T> parser, Function<T,?> eval)
    {
        void test(String input, Object evaluated)
        {
            String inputNoWs = input.replaceAll("\\s+", "");
            List<String> strings = ws ? List.of(input, inputNoWs) : List.of(inputNoWs);
            for(String s : strings)
            {
                T o;
                try
                {
                    o = parser.matchFull(s);
                }
                catch (Exception exception)
                {
                    throw new RuntimeException(exception);
                }
                assertEquals(evaluated, eval.apply(o));
            }
        }
    }

    <T> void testInts(boolean ws, PegParser<T> parser, Function<T,?> eval)
    {
        var tester = new Tester<>(ws, parser, eval);

        tester.test(" 1 ", 1);
        tester.test(" 11 ", 11);
        tester.test(" ( 11 ) ", 11);

        tester.test(" 2 * 3 ", 2*3);
        tester.test(" 2 * ( 3 + 4 ) ", 2*(3+4));
        tester.test(" 2 * ( 3 + 4 ) / ( 5 - 6 )", 2*(3+4)/(5-6));

        tester.test(" 2 + 3 ", 2+3);
        tester.test(" 2 + 3 - 4  ", 2+3-4);
        tester.test(" 2 + ( 3 - 4 )  ", 2+(3-4));
        tester.test(" 2 + ( 3 - 4 ) * 5  ", 2+(3-4)*5);
        tester.test(" 2 * 3 - 4 / 5  ", 2*3-4/5);
    }

    @Test
    void test0()
    {
        testInts(false,
            ExampleParser_Calculator0.parser(),
            ExampleParser_Calculator0.eval());
    }

    @Test
    void test1()
    {
        testInts(true,
            ExampleParser_Calculator1.parser(),
            ExampleParser_Calculator1.eval());
    }

    @Test
    void test2()
    {
        testInts(false,
            ExampleParser_Calculator2.parser(),
            ExampleParser_Calculator2.eval());
    }

    @Test
    void test3()
    {
        testInts(false,
            ExampleParser_Calculator3.parser(),
            ExampleParser_Calculator3.eval());
    }


    @Test
    void test4()
    {
        var tester = new Tester<>(false,
            ExampleParser_Calculator4.parser(),
            ExampleParser_Calculator4.eval()
        );
        // eval: float point, reverse order
        tester.test("1", 1.0);
        tester.test("1+(2+3)*4/5-6", -6.0+1.0/5.0*4.0*(3.0+2.0)+1.0);
    }

}