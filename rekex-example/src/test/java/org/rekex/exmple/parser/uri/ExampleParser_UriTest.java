package org.rekex.exmple.parser.uri;

import org.junit.jupiter.api.Test;
import org.rekex.exmple.parser.ExampleParser_Uri;
import org.rekex.exmple.regexp.ExampleRegExp_UriTest;

class ExampleParser_UriTest
{
    @Test
    void test() throws Exception
    {
        var parser = ExampleParser_Uri.parser();
        String[] examples = ExampleRegExp_UriTest.examples;
        for(String s : examples)
            parser.matchFull(s);
    }
}