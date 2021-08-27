package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.SizeLimit;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.parser.PegParserTestBase;

import java.util.List;

public class PegParser7Test extends PegParserTestBase
{
    // repeat rule, min max

    public enum A
    {
        @Ch("A")a,
    }
    public enum B
    {
        @Ch("B")b,
        @Str("")e, // epsilon
    }

    public record Foo(
        @SizeLimit(min=2, max=4)List<A> listA,
        @SizeLimit(min=2, max=4)List<B> listB
    ){}

    {
        //generateJavaHere=true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        // min, max of A
        matchFail("", 0, 0,0,0);
        matchFail("A", 1, 1,0,0);

        matchFull("AA");
        matchFull("AAA");
        matchFull("AAAA");

        matchPartial("AAAAA", 4);

        // min, max of B

        matchFull("AAB");
        matchFull("AABB");
        matchFull("AABBB");
        matchFull("AABBBB");

        matchPartial("AABBBBB", 6);

    }
}
