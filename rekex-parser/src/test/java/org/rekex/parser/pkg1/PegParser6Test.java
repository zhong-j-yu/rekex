package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.annomacro.AnnoMacro;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Size;
import org.rekex.parser.PegParserTestBase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

public class PegParser6Test extends PegParserTestBase
{
    // repeat rule, array, List

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    public @interface U // size=1
    {
        AnnoMacro<U, Size> toRange = thiz -> AnnoBuilder.build(Size.class, 1);
    }

    public record Obj(@Ch("c")char c){}

    // test code gen for combinations
    //  char Obj =>
    //  char[] Obj[] List<Obj>
    //  char[][] Obj[][] List<Obj>[]
    //  List<char[]> List<Obj[]> List<List<Obj>>
    public record Foo(
        @Ch("c")char @U[] r1,
        Obj @U[] r2,
        @U List<Obj> r3,
        @Ch("c")char @U[] @U[] r4,
        Obj @U[] @U[] r5,
        @U List<Obj> @U[] r6,
        @U List<@Ch("c")char @U[]> r7,
        @U List<Obj @U[]> r8,
        @U List<@U List<Obj>> r9
    ){}

    {
        //generateJavaHere=true;
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        matchFull("c".repeat(9));
    }
}
