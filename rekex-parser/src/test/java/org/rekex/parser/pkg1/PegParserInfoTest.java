package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.ParseInfo;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;

public class PegParserInfoTest extends PegParserTestBase
{
    public enum Bar
    {
        @Str("B1")bar1,
        @Str("B2")bar2,
    }

    public static class Foo
    {
        static final Foo foo = new Foo();

        static Foo print(ParseInfo info, Object... args)
        {
            var input = info.input();
            printf("input=(%d,%d)`%s`, start=%d(%d,%d) end=%d(%d,%d) %n",
                input.start(), input.end(), input.chars(),
                info.start(), info.startLine(), info.startColumn(),
                info.end(), info.endLine(), info.endColumn());
            var info0 = info;
            for(Object arg : args)
            {
                info = info0.of(arg);
                printf(": %d(%d,%d) - %d(%d,%d) %s %n",
                    info.start(), info.startLine(), info.startColumn(),
                    info.end(), info.endLine(), info.endColumn(),
                    info.text());
            }
            for(int i=0; i<info0.argCount(); i++)
            {
                var arg = info0.arg(i);
                info = info0.ofIndex(i);
            }

            return foo;
        }
        static void printf(String format, Object... args)
        {
            String s = format.formatted(args);
            if(false)
                System.out.print(s);
        }

        @Ctor public static Foo a(@Ch("a")Void ch, ParseInfo info)
        {
            return print(info, ch, info);
        }
        @Ctor public static Foo b(@Ch("b")Void ch, Bar bar, ParseInfo info)
        {
            return print(info, ch, bar, info);
        }
        @Ctor public static Foo c(@Ch("c")Void ch, Bar bar, ParseInfo info, Bar bar2, ParseInfo info2)
        {
            print(info, ch, bar, info, bar2, info);
            print(info2, ch, bar, info, bar2, info);
            return foo;
        }
        @Ctor public static Foo d(@Ch("d")Void ch, Bar bar, @Ch("\n")String n, Bar bar2, ParseInfo info)
        {
            print(info, ch, bar, n, bar2, info);
            return foo;
        }

        // check generated source for ctors without ParseInfo
        @Ctor public static Foo y(@Ch("y")Void ch)
        {
            return foo;
        }
        @Ctor public static Foo z(@Ch("z")Void ch, Bar bar)
        {
            return foo;
        }

        @Ctor public static Foo empty(ParseInfo info)
        {
            return print(info);
        }

    }


    {
        //generateJavaHere=true;
        //dumpGrammar = true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        matchFull("a");
        matchFull("bB1");
        matchFull("cB1B2");
        matchFull("dB1\nB2");

        matchFull("");
    }
}
