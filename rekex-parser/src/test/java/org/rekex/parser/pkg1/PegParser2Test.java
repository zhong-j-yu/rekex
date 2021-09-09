package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.annomacro.AnnoMacro;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.helper.anno.StrWs;
import org.rekex.parser.PegParserTestBase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class PegParser2Test extends PegParserTestBase
{
    // regex -> char, int, String, enum

    // U+20000   \uD840\uDC00
    // U+20010   \uD840\uDC10
    // U+20020   \uD840\uDC20
    // U+20030   \uD840\uDC30
    // U+200FF   \uD840\uDCFF

    public record Foo(
        @Ch("abc")char c1,
        @Ch(range={'0','9'})Character c2,
        @StrWs(value={"+","-"}, wsChars=" \t") String s,
        EnumBar bar,
        @Ch("\uD840\uDC10\uD840\uDC20")int i1,
        @Ch(range={0x20000, 0x200FF})Integer i2
    ){}

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    public @interface Word
    {
        String[] value();
        AnnoMacro<Word, StrWs> toStrWs = StrWs.Macro.of(Word::value, " ");
    }


    public enum EnumBar
    {
        @Str("x")x,
        @Word("y")y,
    }

    {
        //generateJavaHere=true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        matchFull("a0+x\uD840\uDC10\uD840\uDC30");
        matchFull("c9- y\uD840\uDC20\uD840\uDC30");

        matchPartial("a0+x\uD840\uDC10\uD840\uDC30~~~", 8);

        //dumpResult=true;
        matchFail("a0+x\uD840\uDC10~~~", 6, 6, 0);
        matchFail("a0*x\uD840\uDC10\uD840\uDC30", 2, 2, 0);
    }
}
