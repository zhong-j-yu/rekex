package org.rekex.helper;

import org.junit.jupiter.api.Test;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.SepBy;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserTestBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SepByTest extends PegParserTestBase
{
    public enum A{ @Str("a")a; }
    public enum Comma{ @Str(",")comma; }

    public record Foo(
        SepBy<A, Comma> as
    ){}

    {
        initParsers(Foo.class, null);
    }

    @Test
    void test()
    {
        //dumpResult=true;
        matchFull("");
        matchFull("a");
        matchFull("a,a");
    }

}
