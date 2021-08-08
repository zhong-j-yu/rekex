package org.rekex.exmple.parser;

import org.rekex.parser.ParseResult;
import org.rekex.parser.PegParser;

import java.util.Scanner;
import java.util.function.Function;

public class ExampleParserUtil
{
    public static <T> void testInputs(String desc, PegParser<T> parser, Function<T,?> eval)
    {
        System.out.println("## testing parser for "+desc);
        var scanner = new Scanner(System.in);
        while(true)
        {
            System.out.println("> input a test string; or 'exit' to exit.");
            if(!scanner.hasNextLine())
                break;
            String line=scanner.nextLine();
            if(line.equals("exit"))
                break;

            var result = parser.parse(line);
            if(result instanceof ParseResult.Full<T> full)
            {
                var value = full.value();
                System.out.println("## parsing succeeded; value: " + value);
                if(eval!=null)
                    System.out.println("eval: "+eval.apply(value));

            }
            else
            {
                System.out.println("## parsing failed; result: "+result);
                if(result instanceof ParseResult.Fatal<T> fatal)
                    fatal.cause().printStackTrace();
            }
        }
    }

}
