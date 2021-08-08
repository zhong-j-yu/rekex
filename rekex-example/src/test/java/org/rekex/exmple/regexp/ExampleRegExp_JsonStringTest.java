package org.rekex.exmple.regexp;

import org.junit.jupiter.api.Test;

class ExampleRegExp_JsonStringTest
{
    @Test
    void test()
    {
        String[] examples = """
            ""
            "a"
            "ab"
            "a\\u1234b"
            "\\uD800\\uDC00"
            "a\\\\b"
            "a\\"b"
            "a\\tb"
            """.split("\\n");
        ExampleRegExpTestBase.matches(examples, ExampleRegExp_JsonString.exp());

    }
}