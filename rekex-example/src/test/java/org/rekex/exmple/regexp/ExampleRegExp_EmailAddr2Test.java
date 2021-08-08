package org.rekex.exmple.regexp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExampleRegExp_EmailAddr2Test
{
    @Test
    void test()
    {
        String[] examples = {
            "zhong.j.yu@gmail.com",
            ".a.b@1", //
            "x@abc", //
            "x@a.b-c.com", //
        };
        var exp = ExampleRegExp_EmailAddr2.exp();
        ExampleRegExpTestBase.matches(examples, exp);
    }
}