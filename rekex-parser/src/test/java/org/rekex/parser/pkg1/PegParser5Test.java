package org.rekex.parser.pkg1;

import org.junit.jupiter.api.Test;
import org.rekex.spec.Ctor;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParserTestBase;

public class PegParser5Test extends PegParserTestBase
{
    // fail positions
    public static class Foo
    {
        @Ctor public static Foo
        ctorA(@Ch("XY")char c1, @Ch("=")char c2, @Ch("A")char c3, Never never)
        {
            return null;
        }

        @Ctor public static Foo
        ctorB(@Ch("YZ")char c1, @Ch("=")char c2, @Ch("B")char c3, Never never)
        {
            return null;
        }
    }
    public enum Never
    {
        @Ch("")I  // never matches
    }

    {
        //generateJavaHere=true;
        initParsers(Foo.class);
    }

    @Test
    void test()
    {
        matchFail("W=C", 0, 0,0,0);
        matchFail("X=A", 3, 3,0,0);
        matchFail("Y=A", 3, 3,0,0);
        matchFail("Y=B", 3, 3,0,0);

        //dumpResult=true;
        // both failed at the same position;
        // ruleA is chosen to be reported as root cause.
        matchFail("Y=", 2, 2,0,0);
        matchFail("Y=C", 2, 2,0,0);
    }
}
