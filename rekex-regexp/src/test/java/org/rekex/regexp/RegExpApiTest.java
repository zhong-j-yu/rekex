package org.rekex.regexp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.rekex.regexp.RegExp.*;

import static org.rekex.regexp.RegExpApi.*;

import java.util.regex.Pattern;

// see RegExpGroupRefTest for group and back-ref
class RegExpApiTest extends RegExpTestBase
{
    @Test void test_ch()
    {
        
        test(ch(""))
            .regex("[0&&1]")
        ;
        test(ch("a"))
            .regex("a")
        ;
        test(ch("ab"))
            .regex("[ab]")
        ;

        test(intersect())
            .regex("[\\u0000-\\x{10FFFF}]")
        ;
        testSimplify(intersect().except('a', 'b'))
            .regex("[^ab]")
        ;

        var beta = predefined("\\N{GREEK SMALL LETTER BETA}");
        test(seq("A", beta, "C"))
            .regex("A\\N{GREEK SMALL LETTER BETA}C")
            .matches("A\u03B2C");

    }

    @Test void test_alt_to_ch()
    {
        test(alt('a'))
            .type(CharClass.Single.class)
            .regex("a")
        ;
        test(alt(0x20))
            .type(CharClass.Single.class)
            .regex("\\ ")
        ;
        test(alt("a"))
            .type(CharClass.Single.class)
            .regex("a")
        ;
        test(alt())
            .type(Alternation.class)
            .regex("(?!)")
        ;
        testSimplify(alt(0x20, 'a', "b", range('c', 'd'), seq('e'), seq("f"), union(), alt()))
            .type(CharClass.Union.class)
            .regex("[\\ abc-def]")
        ;
    }
    @Test void test_seq_to_ch()
    {
        test(seq('a'))
            .type(CharClass.Single.class)
            .regex("a")
            ;
        test(seq(0x20))
            .type(CharClass.Single.class)
            .regex("\\ ")
            .matches(" ")
        ;
        test(seq("a"))
            .type(CharClass.Single.class)
            .regex("a")
            ;
        testSimplify(seq(seq(), seq(""), range('x', 'y'), "", alt("")))
            .type(CharClass.Range.class)
            .regex("[x-y]");
    }

    @Test void test__range()
    {
        test(range(0x20, 'Z'))
            .regex("[\\ -Z]");
    }

    @Test void test_unicode()
    {
        // U+20000   \uD840\uDC00
        // U+20010   \uD840\uDC10
        // U+20020   \uD840\uDC20
        // U+20030   \uD840\uDC30
        // U+200FF   \uD840\uDCFF

        test(seq('=', 0x20010, "=\uD840\uDC20=", range(0x2000, 0x200FF), "=" ))
            .regex("=\\x{20010}=\\x{20020}=[\\u2000-\\x{200FF}]=")
            .matches("=\uD840\uDC10=\uD840\uDC20=\uD840\uDC30=")
        ;
    }

    @Test void test__union()
    {
        test(union())
            .regex("[0&&1]")
        ;
        test(union('a'))
            .regex("a")
        ;
        test(union('a', (int)'b'))
            .regex("[ab]")
        ;
        test(union(ch("ab"), ch("cd")))
            .regex("[abcd]")
        ;
        test(union(ch("ab"), range('c', 'e')))
            .regex("[abc-e]")
            .matches("a", "d")
        ;
    }

    @Test void test__intersect()
    {
        test(intersect())
            .regex("[\\u0000-\\x{10FFFF}]")
        ;
        test(intersect('a'))
            .regex("a")
        ;
        test(intersect('b', range('a', 'c')))
            .regex("[b&&a-c]")
            .matches("b")
            .noMatch("a", "c")
        ;
        test(intersect('a', intersect('b', 'c')))
            .regex("[a&&b&&c]")
        ;
    }

    @Test void test_negate0()
    {
        test(negate(union()))
            .regex("[^0&&1]")
            .matches("A")
        ;
        test(negate(alt()))
            .regex("[^0&&1]")
            .matches("A")
        ;
        test(negate(ch("")))
            .regex("[^0&&1]")
            .matches("A")
        ;
    }
    @Test void test_negate1()
    {
        test(negate('A'))
            .regex("[^A]")
            .noMatch("A");
    }
    @Test void test_negate()
    {
        var r = range('A', 'Z');
        var neg = negate(r);
        var neg2 = negate(neg);
        test(neg).regex("[^A-Z]");
        test(neg2).regex("[^[^A-Z]]");
        testSimplify(neg2).regex("[A-Z]");
    }

    @Test void test__predefined()
    {
        test(predefined("\\d"))
            .regex("\\d")
            .matches("0", "1");
    }


    @Test void testOneArg()
    {
        var r = seq(alt(intersect(union(('A')))));
        test(r)
            .regex("A")
            .type(CharClass.Single.class)
        ;
    }




    @Test void test_alt()
    {
        var r = alt(
            (int)'A',
            'B',
            "CD",
            alt(
                "11",
                "22"
            ),
            seq("XYZ")
        );
        test(r)
            .regex("A|B|CD|11|22|XYZ");

    }

    @Test void test_except()
    {
        var r = alt(
            'A',
            ch("BC"),
            range('D', 'F'),
            seq("G")
        );
        test(r)
            .regex("A|[BC]|[D-F]|G");
        
        test(r.except())
            .regex("[ABCD-FG]");

        test(r.except('E', 'Z'))
            .regex("[ABCD-FG&&[^EZ]]")
            .matches("A", "D", "G")
            .noMatch("E", "H", "Z")
        ;
    }

    @Test void test_except2()
    {
        // predefined character class in union/intersect/negate
        var r = alt(
            "+",
            '-',
            predefined("\\w")
        ).except(
            '+',
            '0',
            predefined("\\p{Upper}")
        );
        test(r)
            .regex("[\\+\\-\\w&&[^\\+0\\p{Upper}]]")
            .noMatch("+", "0", "A")
            .matches("-", "1", "a");
    }

    @Test void test_seq()
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
        test(r)
            .regex("ABCD(?:11|22)XYZ");
    }



    @Test void test_quant()
    {
        test(opt("A", "B"))
            .regex("(?:AB)?");
        test(rep0("A", "B"))
            .regex("(?:AB)*");
        test(rep1("A", "B"))
            .regex("(?:AB)+");
        test(times(2, seq("A", "B")))
            .regex("(?:AB){2}");
        test(times(2, Long.MAX_VALUE, seq("A", "B")))
            .regex("(?:AB){2,}");
        test(times(2, 4, seq("A", "B")))
            .regex("(?:AB){2,4}");

        test(reluctant(opt("A", "B")))
            .regex("(?:AB)??");
        test(reluctant(rep0("A", "B")))
            .regex("(?:AB)*?");
        test(reluctant(rep1("A", "B")))
            .regex("(?:AB)+?");
        test(reluctant(times(2, seq("A", "B"))))
            .regex("(?:AB){2}?");
        test(reluctant(times(2, Long.MAX_VALUE, seq("A", "B"))))
            .regex("(?:AB){2,}?");
        test(reluctant(times(2, 4, seq("A", "B"))))
            .regex("(?:AB){2,4}?");

        test(possessive(opt("A", "B")))
            .regex("(?:AB)?+");
        test(possessive(rep0("A", "B")))
            .regex("(?:AB)*+");
        test(possessive(rep1("A", "B")))
            .regex("(?:AB)++");
        test(possessive(times(2, seq("A", "B"))))
            .regex("(?:AB){2}+");
        test(possessive(times(2, Long.MAX_VALUE, seq("A", "B"))))
            .regex("(?:AB){2,}+");
        test(possessive(times(2, 4, seq("A", "B"))))
            .regex("(?:AB){2,4}+");

        // quant(quant())

        test(opt(opt("A")))
            .regex("(?:A?)?");
        testSimplify(opt(opt("A")))
            .regex("A?");
    }




    


    @Test void test__lookaround()
    {
        var L = range('A', 'Z');
        test(seq(lookahead(true, seq("AB")), L, L, L))
            .regex("(?=AB)[A-Z][A-Z][A-Z]")
            .matches("ABC")
            .noMatch("AAA")
            ;
        test(seq(lookahead(false, seq("AB")), L, L, L))
            .regex("(?!AB)[A-Z][A-Z][A-Z]")
            .noMatch("ABC")
            .matches("AAC", "BAC", "BBC")
            ;
        test(seq(L, L, L, lookbehind(true, seq("BC"))))
            .regex("[A-Z][A-Z][A-Z](?<=BC)")
            .matches("ABC")
            .noMatch("CCC")
            ;
        test(seq(L, L, L, lookbehind(false, seq("BC"))))
            .regex("[A-Z][A-Z][A-Z](?<!BC)")
            .noMatch("ABC")
            .matches("ABB", "ACB", "ACC")
            ;

        var digit = range('0', '9');
        var notLedByZero = lookahead(false, seq("0", digit));
        var number = seq(notLedByZero, rep1(digit));
        test(number)
            .matches("0", "1", "10")
            .noMatch("00", "01", "001", "010");
    }


    @Test void test__flag()
    {
        var a = alt('a');

        test(flag(true, 0, a))
            .regex("(?:a)")  // just a non-capturing group
            .matches("a")
            .noMatch("", "A")
        ;
        if(false) System.out.println(toTreeText(flag(true, 0, a)));

        assertThrows(IllegalArgumentException.class, () -> {
            flag(true, Pattern.LITERAL, a); // no embedded flag
        });
        assertThrows(IllegalArgumentException.class, () -> {
            flag(true, Pattern.CANON_EQ, a); // no embedded flag
        });

        test(ignoreCase(a))
            .regex("(?i:a)")
            .matches("a", "A")
            .noMatch("", "b")
            ;

        var nested = ignoreCase(
            seq(
                "a",
                flag(false, Pattern.CASE_INSENSITIVE, alt('a'))
            )
        );
        test(nested)
            .regex("(?i:a(?-i:a))")
            .matches("aa", "Aa")
            .noMatch("aA", "AA")
            ;

    }


    @Test void test__boundary()
    {
        var any = intersect();
        var WB = boundary_word_boundary();
        var r = seq(rep1(any), WB, rep1(any));
        test(r)
            .regex("[\\u0000-\\x{10FFFF}]+\\b[\\u0000-\\x{10FFFF}]+")
            .matches("A B")
            .noMatch("AB")
        ;


        alt(
            boundary("\\b{g}"),
            boundary_beginning_of_line(),
            boundary_end_of_line(),
            boundary_word_boundary(),
            boundary_non_word_boundary(),
            boundary_beginning_of_input(),
            boundary_end_of_previous_match(),
            boundary_end_of_input_but_for_final_terminator(),
            boundary_end_of_input()
        );
    }

    @Test void test__opaque()
    {
        test(opaque("\\X"))
            .matches("\u0061\u0300", "\u0061")
        ;

        var R = opaque("\\R");
        test(R)
            .matches("\r", "\n", "\r\n")
            .noMatch("\n\r")
        ;
        test(new Concatenation(R, R))
            .regex("(?:\\R)(?:\\R)")  // paren-ed, because we don't know the Opaque is atomic
            .matches("\r\r", "\n\n", "\r\n\r\n")
            .matches("\r\n") // hmm... it doesn't treat \r\n as atomic
        ;
    }


}