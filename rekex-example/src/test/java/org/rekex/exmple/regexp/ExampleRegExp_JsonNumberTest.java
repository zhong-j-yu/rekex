package org.rekex.exmple.regexp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExampleRegExp_JsonNumberTest
{
    @Test
    void test()
    {
        String[] examples = {
            "0", "1", "10",
            "-0", "-1", "-10",
            "1.0",
            "-1.23E10",
            "-1.23e+03", // leading zero is ok in component?
            "10e-1"
        };

        ExampleRegExpTestBase.matches(examples, ExampleRegExp_JsonNumber.exp());
        ExampleRegExpTestBase.matches(examples, ExampleRegExp_JsonNumber.exp0());
    }
}