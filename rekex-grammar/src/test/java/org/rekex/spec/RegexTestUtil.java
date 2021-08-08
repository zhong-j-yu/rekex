package org.rekex.spec;

import org.rekex.annomacro.AnnoMacro;

import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTestUtil
{
    public static final AnnoMacro.Resolver resolver = new AnnoMacro.Resolver();

    public static void test(Annotation anno, String input, String result)
    {
        Regex regex = resolver.resolve(anno);
        Pattern pattern = Pattern.compile(regex.value(), regex.flags());
        var matcher = pattern.matcher(input);
        String g;
        if(matcher.matches())
            g = matcher.group(regex.group()); // could be null
        else
            g = null;
        assertEquals(result, g);
    }

}
