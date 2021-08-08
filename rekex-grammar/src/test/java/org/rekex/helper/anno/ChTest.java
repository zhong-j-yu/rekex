package org.rekex.helper.anno;

import org.junit.jupiter.api.Test;
import org.rekex.spec.RegexTestUtil;
import org.rekex.common_util.AnnoBuilder;

public class ChTest
{
    @Test
    void test1()
    {
        test(false, "", "", false);
        test(false, "", "a", false);

        test(false, "abc", "b", true);

        test(false, "abc", "", false);
        test(false, "abc", "z", false);
        test(false, "abc", "ab", false);
    }
    @Test
    void testIgnoreCase()
    {
        test(true, "abc", "b", true);
        test(true, "abc", "B", true);
        test(true, "abc", "z", false);
        test(true, "abc", "Z", false);
    }

    void test(boolean ignoreCase, String value, String input, boolean match)
    {
        Ch a1 = AnnoBuilder.of(Ch.class)
            .def(Ch::value, value)
            .def(Ch::ignoreCase, ignoreCase)
            .build();
        RegexTestUtil.test(a1, input, match?input:null);
    }
}
