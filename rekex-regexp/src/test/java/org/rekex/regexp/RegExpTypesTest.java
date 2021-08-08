package org.rekex.regexp;

import org.junit.jupiter.api.Test;
import org.rekex.regexp.RegExp.CharClass.*;
import org.rekex.regexp.RegExp.*;

import static org.junit.jupiter.api.Assertions.*;

// test RegExp types directly.

class RegExpTypesTest extends RegExpTestBase
{
    @Test void alternation()
    {
        test(new Alternation())
            .regex("(?!)")
            .noMatch("", "a")
        ;
        var a = new Single('a');
        test(new Alternation(a))
            .regex("a")
            .matches("a")
            .noMatch("", "b")
        ;
        var b = new Single('b');
        var ab = new Alternation(a, b);
        test(ab)
            .regex("a|b")
            .matches("a", "b")
            .noMatch("", "c")
        ;
        testSimplify(ab)
            .regex("[ab]")
            .matches("a", "b")
            .noMatch("", "c")
        ;
        var cd = new Range('c','d');
        test(new Alternation(cd, ab))
            .regex("[c-d]|a|b")
            .matches("a", "b", "c", "d")
            .noMatch("", "e")
        ;
        testSimplify(new Alternation(cd, ab))
            .regex("[c-dab]")
            .matches("a", "b", "c", "d")
            .noMatch("", "e")
        ;
        var E = new Concatenation();
        var F = RegExpApi.opaque("bc");
        test(new Alternation(E, a, F))
            .regex("|a|bc")
            .matches("", "a", "bc")
            .noMatch("b")
        ;
    }

    @Test void concatenation()
    {
        test(new Concatenation())
            .regex("")
            .matches("")
            .noMatch(" ")
        ;
        var a = new Single('a');
        test(new Concatenation(a))
            .regex("a")
            .matches("a")
            .noMatch("", "b")
        ;
        var b = new Single('b');
        var ab = new Concatenation(a,b);
        test(ab)
            .regex("ab")
            .matches("ab")
            .noMatch("", "a")
        ;
        var c = new Single('c');
        test(new Concatenation(c, ab))
            .regex("cab")
            .matches("cab")
        ;
        // precedence
        var aOpt = new Quantified(a, 0, 1, Quantified.greedy);
        test(new Concatenation(a, aOpt, new Alternation(ab,c), RegExpApi.opaque("de")))
            .regex("aa?(?:ab|c)(?:de)")
            .matches("aabde","acde", "aaabde","aacde")
        ;

    }

    @Test void quantified()
    {
        final char greedy = Quantified.greedy;
        final long INF = Quantified.INFINITY;

        var a = new Single('a');
        // 0 <= min <= max
        assertThrows(IllegalArgumentException.class, ()->{
            new Quantified(a, -1, 1, greedy);
        });
        assertThrows(IllegalArgumentException.class, ()->{
            new Quantified(a, 2, 1, greedy);
        });

        test(new Quantified(a, 0, 1, greedy))
            .regex("a?")
            .matches("", "a")
            .noMatch("aa")
        ;
        test(new Quantified(a, 0, INF, greedy))
            .regex("a*")
            .matches("", "a", "aa")
            .noMatch("ab")
        ;
        test(new Quantified(a, 1, INF, greedy))
            .regex("a+")
            .matches("a", "aa")
            .noMatch("", "ab")
        ;
        test(new Quantified(a, 2, 2, greedy))
            .regex("a{2}")
            .matches("aa")
            .noMatch("", "a", "aaa")
        ;
        test(new Quantified(a, 2, INF, greedy))
            .regex("a{2,}")
            .matches("aa", "aaa")
            .noMatch("", "a")
        ;
        test(new Quantified(a, 2, 3, greedy))
            .regex("a{2,3}")
            .matches("aa", "aaa")
            .noMatch("", "a", "aaaa")
        ;

        // precedence
        var b = new Single('b');
        var ab = new Concatenation(a,b);
        test(new Quantified(ab, 0, INF, greedy))
            .regex("(?:ab)*")
            .matches("", "ab", "abab")
        ;

        // greediness
        var aPlus = new Quantified(a, 1, INF, greedy);
        test(aPlus)
            .regex("a+")
            .lookingAt("aabb", "aa")  // greedy
        ;
        var reluctant = new Quantified(a, 1, INF, Quantified.reluctant);
        test(reluctant)
            .regex("a+?")
            .lookingAt("aabb", "a")
        ;

        var possessive = new Quantified(a, 1, INF, Quantified.possessive);
        test(possessive)
            .regex("a++")
            .lookingAt("aabb", "aa")
        ;
        test(new Concatenation(possessive, new Single('a')))
            .regex("a++a")
            .noMatch("aa", "aaa");   // a++ consumed all, does not backtrack.

        // quant(quant)
        var qq = new Quantified(aPlus, 1, INF, greedy);
        test(new Concatenation(qq, a))
            .regex("(?:a+)+a")
            .matches("aa", "aaa")
            .noMatch("", "a")
        ;
        testSimplify(new Concatenation(qq, a))
            .regex("a+a")
            .matches("aa", "aaa")
            .noMatch("", "a")
        ;

    }

    @Test void flag()
    {
        var a = new Single('a');

        test(new Flagged(a, "", ""))
            .regex("(?:a)")
            .matches("a")
            .noMatch("A");

        var f1 = new Flagged(a, "", "i");
        var seq = new Concatenation(a, f1);
        var f2 = new Flagged(seq, "i", "");
        test(f2)
            .regex("(?i:a(?-i:a))")
            .matches("Aa", "aa")
            .noMatch("AA", "aA")
        ;
        // both on and off
        var f3 = new Flagged(a, "i", "i");
        test(f3)
            .regex("(?i-i:a)")
            .matches("a")
            .noMatch("A")
        ;
        var f4 = new Flagged(f3, "i", "");
        test(f4)
            .regex("(?i:(?i-i:a))")
            .matches("a")
            .noMatch("A")
        ;

        // multiple flags
        var f5 = new Flagged(
            new Concatenation(a, new Predefined(".")),
            "is", "dm");
        test(f5)
            .regex("(?is-dm:a.)")
            .matches("A\n")
        ;

    }





}