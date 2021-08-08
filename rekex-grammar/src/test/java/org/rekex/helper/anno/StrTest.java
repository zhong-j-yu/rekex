package org.rekex.helper.anno;

import org.junit.jupiter.api.Test;
import org.rekex.spec.RegexTestUtil;
import org.rekex.common_util.AnnoBuilder;

public class StrTest
{
    @Test
    void test1()
    {
        test(false, v(), "", false);
        test(false, v(""), "", true);

        test(false, v("abc","xyz"), "abc", true);
        test(false, v("abc","xyz"), "xyz", true);

        test(false, v("abc","xyz"), "ab", false);
        test(false, v("abc","xyz"), "abcd", false);

    }
    @Test
    void testIgnoreCase()
    {
        test(true, v("abc","xyz"), "abc", true);
        test(true, v("abc","xyz"), "ABC", true);
    }

    static String[] v(String... arr)
    {
        return arr;
    }
    void test(boolean ignoreCase, String[] value, String input, boolean match)
    {
        Str a1 = AnnoBuilder.of(Str.class)
            .def(Str::value, value)
            .def(Str::ignoreCase, ignoreCase)
            .build();
        RegexTestUtil.test(a1, input, match?input:null);
    }
}
