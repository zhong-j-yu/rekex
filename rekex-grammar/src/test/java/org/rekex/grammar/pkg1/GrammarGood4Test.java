package org.rekex.grammar.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.grammar.GrammarTestBase;
import org.rekex.helper.anno.AtLeast;
import org.rekex.helper.anno.AtMost;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.spec.Ctor;
import org.rekex.spec.Permits;
import org.rekex.spec.Regex;

import java.util.List;

public class GrammarGood4Test extends GrammarTestBase
{
    // both @Permits on class and @Ctor in class body
    // ctors takes precedence

    @Permits({A1.class, A2.class})
    public interface A
    {
        @Ctor public static
        A a1(@Ch("1")char c){ return null; }

        static
        A a2(@Ch("2")char c){ return null; }
    }
    public record A1(@Ch("1")char c) implements A{}
    public record A2(@Ch("2")char c) implements A{}

    @Test
    void test() throws Exception
    {
        var grammar = derive(A.class);
        //dump(grammar);
    }

}
