package org.rekex.exmple.regexp;

import org.opentest4j.AssertionFailedError;
import org.rekex.regexp.RegExp;
import org.rekex.regexp.RegExpApi;

import java.util.regex.Pattern;

class ExampleRegExpTestBase
{
    static void matches(String[] examples, RegExp exp)
    {
        String regex = RegExpApi.toRegex(exp);
        Pattern pattern = Pattern.compile(regex);
        for(var str : examples)
        {
            boolean matches = pattern.matcher(str).matches();
            if(!matches)
                throw new AssertionFailedError("regex failed to match input: "+str);
        }
    }
}
