package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import java.util.Scanner;
import java.util.regex.Pattern;

import static org.rekex.regexp.RegExpApi.*;

class ExampleRegExpUtil
{
    static void dump(RegExp exp)
    {
        System.out.println("#### RegExp");
        System.out.println(toTreeText(exp));
        System.out.println(toRegex(exp));
        System.out.println();
        {
            var exp2 = simplify(exp);
            System.out.println("#### RegExp simplified");
            System.out.println(toTreeText(exp2));
            System.out.println(toRegex(exp2));
        }
        System.out.println();
    }

    static void testInputs(RegExp exp, String name)
    {
        dump(exp);

        System.out.println("#### Test regex for "+name);

        Pattern pattern = Pattern.compile(toRegex(exp));

        var scanner = new Scanner(System.in);
        while(true)
        {
            System.out.println("> input a test string; or 'exit' to exit.");
            if(!scanner.hasNextLine())
                break;
            String line=scanner.nextLine();
            if(line.equals("exit"))
                break;

            var matcher = pattern.matcher(line);
            System.out.println("matches="+matcher.matches());
        }
    }

}
