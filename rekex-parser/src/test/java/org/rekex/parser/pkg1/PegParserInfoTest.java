package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.ParseInfo;
import org.rekex.parser.PegParserTestBase;
import org.rekex.spec.Ctor;
import org.rekex.spec.Not;

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
            if(false)
            {
                System.out.printf("input=%s, start=%d(%d,%d) end=%d(%d,%d) %n", info.input(),
                    info.start(), info.startLine(), info.startColumn(),
                    info.end(), info.endLine(), info.endColumn());
                var info0 = info;
                for(Object arg : args)
                {
                    info = info0.of(arg);
                    System.out.printf(": %d(%d,%d) - %d(%d,%d) %s %n",
                        info.start(), info.startLine(), info.startColumn(),
                        info.end(), info.endLine(), info.endColumn(),
                        info.text());
                }
            }
            return foo;
        }

        @Ctor public static Foo a(@Ch("a")String ch, ParseInfo info)
        {
            return print(info, ch, info);
        }
        @Ctor public static Foo b(@Ch("b")String ch, Bar bar, ParseInfo info)
        {
            return print(info, ch, bar, info);
        }
        @Ctor public static Foo c(@Ch("c")String ch, Bar bar, ParseInfo info, Bar bar2, ParseInfo info2)
        {
            print(info, ch, bar, info, bar2, info);
            print(info2, ch, bar, info, bar2, info);
            return foo;
        }
        @Ctor public static Foo d(@Ch("d")String ch, Bar bar, @Ch("\n")String n, Bar bar2, ParseInfo info)
        {
            print(info, ch, bar, n, bar2, info);
            return foo;
        }

        // check generated source for ctors without ParseInfo
        @Ctor public static Foo y(@Ch("y")String ch)
        {
            return foo;
        }
        @Ctor public static Foo z(@Ch("z")String ch, Bar bar)
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
