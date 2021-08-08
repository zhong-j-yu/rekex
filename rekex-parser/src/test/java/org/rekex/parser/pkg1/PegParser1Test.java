package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.parser.PegParserTestBase;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.annotype.PrimitiveType;
import org.rekex.helper.anno.Ch;

import java.util.List;
import java.util.Set;

public class PegParser1Test extends PegParserTestBase
{
    {
        // primitive type as root type
        // @Ch("abc")char
        var anno = AnnoBuilder.build(Ch.class, "abc");
        var type = new PrimitiveType(Set.of(anno), char.class);
        //generateJavaHere=true;
        initParsers(type, null);
        //parsers = List.of(new PegParser1Test__Gen());
    }

    @Test
    void test()
    {
        matchFull("a");
        matchFull("c");

        matchPartial("ax", 1);
        matchPartial("ab", 1);

        //dumpResult=true;
        matchFail("", 0, 0);
        matchFail("x", 0, 0);
    }
}
