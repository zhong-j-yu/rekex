package org.rekex.regexp;

import org.junit.jupiter.api.Test;

import static org.rekex.regexp.RegExpApi.*;

class ToTreeTextTest extends RegExpTestBase
{
    @Test
    void trees()
    {
        {
            var r = seq(
                predefined("\\d"),
                predefined("\\p{Lower}"),
                boundary_word_boundary(),
                boundary("\\b{g}"),
                opaque("\\R"),
                opaque("\\X"),
                seq()
            );
            test(r).dump();
        }
        {
            var r = group(seq("AB", group("g2", alt("CD", "EF"))));
            test(r).dump();
        }
        {
            var r = seq(
                group(seq("A")),
                group("g2", seq("B")),
                backRef(1),
                backRef("g2")
            );
            test(r).dump();
        }
        {
            var ag = atomicGroup(alt("AB", "A"));
            var r = seq(ag, "B");
            test(r).dump();
        }
        {
            var r = seq(
                lookahead(true, seq("A")),
                lookahead(false, seq("B")),
                lookbehind(true, seq("C")),
                lookbehind(false, seq("D"))
            );
            test(r).dump();
        }
        {
            var r = seq(
                (int)'A',
                'B',
                "CD",
                alt(
                    "11",
                    "22"
                ),
                seq("XYZ")
            );
            test(r).dump();
            testSimplify(r).dump();
        }
        {
            var r = seq("A", ignoreCase(alt("B", "C")));
            test(r).dump();
        }

    }

    @Test
    void quantified()
    {
        {
            var r = seq(
                opt("ABC"),
                rep0("ABC"),
                rep1("ABC"),
                times(3, seq("ABC")),
                times(3, Long.MAX_VALUE, seq("ABC")),
                times(3, 6, seq("ABC"))
            );
            test(r).dump();
        }
        {
            var r = seq(
                reluctant(opt("ABC")),
                reluctant(rep0("ABC")),
                reluctant(rep1("ABC")),
                reluctant(times(3, seq("ABC"))),
                reluctant(times(3, Long.MAX_VALUE, seq("ABC"))),
                reluctant(times(3, 6, seq("ABC")))
            );
            test(r).dump();
        }
        {
            var r = seq(
                possessive(opt("ABC")),
                possessive(rep0("ABC")),
                possessive(rep1("ABC")),
                possessive(times(3, seq("ABC"))),
                possessive(times(3, Long.MAX_VALUE, seq("ABC"))),
                possessive(times(3, 6, seq("ABC")))
            );
            test(r).dump();
        }
    }
}