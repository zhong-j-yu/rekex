package org.rekex.exmple.regexp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExampleRegExp_EmailAddr1Test
{
    @Test
    void test()
    {
        String[] examples = {
            "zhong.j.yu@gmail.com",
        };
        var exp = ExampleRegExp_EmailAddr1.exp();
        ExampleRegExpTestBase.matches(examples, exp);
    }
}