package org.rekex.helper.anno;

import org.junit.jupiter.api.Test;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.spec.Regex;
import org.rekex.spec.RegexTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class ChRangeTest
{
    @Test
    void test1()
    {
        test(false, 'a', 'z', "", "a", true);
        test(false, 'a', 'z', "", "A", false);

        test(false, 'a', 'z', "mn", "a", true);
        test(false, 'a', 'z', "mn", "m", false);

        test(false, 'a', 'z', "MN", "m", true);
    }
    @Test
    void testIgnoreCase()
    {
        test(true, 'a', 'z', "mn", "a", true);
        test(true, 'a', 'z', "mn", "A", true);

        test(true, 'a', 'z', "mn", "m", false);
        test(true, 'a', 'z', "mn", "M", false);
        test(true, 'a', 'z', "MN", "m", false);
        test(true, 'a', 'z', "MN", "M", false);
    }

    void test(boolean ignoreCase, char from, char to, String except, String input, boolean match)
    {
        Ch a1 = AnnoBuilder.of(Ch.class)
            .def(Ch::range, new int[]{from, to})
            .def(Ch::except, except)
            .def(Ch::ignoreCase, ignoreCase)
            .build();
        RegexTestUtil.test(a1, input, match?input:null);
    }

    @Test
    void testValueAndRange()
    {
        Ch a1 = AnnoBuilder.of(Ch.class)
            .def(Ch::value, "xyz")
            .def(Ch::range, new int[]{'0','9', 'A', 'F'})
            .def(Ch::except, "Y")
            .def(Ch::ignoreCase, true)
            .build();
        Regex r = RegexTestUtil.resolver.resolve(a1);
        assertEquals(r.value(), "(?i:[xyz0-9A-F&&[^Y]])");

        RegexTestUtil.test(r, "X", "X");
        RegexTestUtil.test(r, "0", "0");
        RegexTestUtil.test(r, "A", "A");
        RegexTestUtil.test(r, "b", "b");
        RegexTestUtil.test(r, "y", null);
    }

}
