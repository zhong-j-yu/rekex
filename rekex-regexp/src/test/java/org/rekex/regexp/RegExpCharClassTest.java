package org.rekex.regexp;

import org.junit.jupiter.api.Test;
import org.rekex.regexp.RegExp.CharClass.*;
import static org.junit.jupiter.api.Assertions.*;

// test CharClass types directly.
// some structures are not accessible from RegExpApi, e.g. Union of 1 arg

class RegExpCharClassTest extends RegExpTestBase
{
    @Test void single()
    {
        test(new Single('a'))
            .regex("a")
            .matches("a")
            .noMatch("", "b", "aa")
        ;
        test(new Single('\\'))
            .regex("\\\\")
            .matches("\\")
        ;
        test(new Single('\u0001'))
            .regex("\\u0001")
            .matches("\u0001")
        ;
        test(new Single('\u1ACE'))
            .regex("\\u1ACE")
            .matches("\u1ACE")
        ;
        test(new Single('\n'))
            .regex("\\n")
            .matches("\n")
        ;
        test(new Single('['))
            .regex("\\[")
            .matches("[")
        ;
    }

    @Test void range()
    {
        test(new Range('a', 'z'))
            .regex("[a-z]")
            .matches("a", "b", "z")
            .noMatch("", " ", "1", "~", "aa")
        ;
        test(new Range('\u0000', 0x10FFFF))
            .regex("[\\u0000-\\x{10FFFF}]")
            .matches("\u0000", "\uDBFF\uDFFF")
        ;

        test(new Range('a', 'a')) // allowed
            .regex("[a-a]")
        ;

        assertThrows(IllegalArgumentException.class, ()->{
            new Range('Z', 'A');
        });
    }

    @Test void union()
    {
        var r1 = new Range('A', 'Z');
        var r2 = new Range('a', 'z');

        test(new Union())
            .regex("[0&&1]")
            .noMatch("0", "1")
        ;
        test(new Union(r1))
            .regex(ToRegex.regex(r1))
        ;
        test(new Union(r1, r2))
            .regex("[A-Za-z]")
            .matches("a", "Z")
        ;
        test(new Union(r1, new Single('0'), r2, new Single('1')))
            .regex("[A-Z0a-z1]")
            .matches("a", "Z", "0", "1")
        ;
    }

    @Test void intersection()
    {
        var r1 = new Range('A', 'M');
        var r2 = new Range('N', 'Z');
        var r3 = new Range('B', 'Y');

        test(new Intersection())
            .regex("[\\u0000-\\x{10FFFF}]")
            .matches("\u0000", "\uDBFF\uDFFF")
        ;
        test(new Intersection(r1))
            .regex(ToRegex.regex(r1))
        ;
        test(new Intersection(r1,r2))
            .regex("[A-M&&N-Z]")
            .noMatch("M", "N");
        ;
        test(new Intersection(r1,r3))
            .regex("[A-M&&B-Y]")
            .matches("B", "M")
            .noMatch("A", "Y")
        ;
    }

    @Test void negation()
    {
        test(new Negation(new Single('A')))
            .regex("[^A]")
            .noMatch("A")
            .matches("B")
        ;
        test(new Negation(new Range('A','Z')))
            .regex("[^A-Z]")
            .noMatch("A", "Z")
            .matches("1")
        ;
        test(new Negation(new Union(new Range('A','Z'), new Range('a', 'z'))))
            .regex("[^A-Za-z]")
            .noMatch("A", "z")
            .matches("1")
        ;
        test(new Negation(new Intersection(new Range('A','M'), new Range('B', 'Y'))))
            .regex("[^A-M&&B-Y]")
            .noMatch("B", "M")
            .matches("A", "Y")
        ;
        test(new Intersection(new Range('A','M'), new Negation(new Range('B', 'Y'))))
            .regex("[A-M&&[^B-Y]]")
            .noMatch("B", "M", "Y")
            .matches("A")
        ;
        {
            var neg = new Negation(new Single('A'));
            var neg2 = new Negation(neg);
            test((neg2)).regex("[^[^A]]").matches("A");
            testSimplify(neg2).regex("A").matches("A");
            var neg3 = new Negation(neg2);
            test((neg3)).regex("[^[^[^A]]]").noMatch("A");
            testSimplify(neg3).regex("[^A]").noMatch("A");
        }
        {
            var neg = new Negation(new Union(new Single('A'), new Single('B')));
            var neg2 = new Negation(neg);
            var neg3 = new Negation(neg2);
            testSimplify(neg2).regex("[AB]").matches("A","B");
            testSimplify(neg3).regex("[^AB]").noMatch("A","B");
        }
    }

    @Test void except()
    {
        test(new Range('A', 'Z').except())
            .regex("[A-Z]")
            .matches("A", "Z")
        ;
        test(new Range('A', 'Z').except(new Union()))
            .regex("[A-Z&&[^0&&1]]")
            .matches("A", "Z")
        ;
        test(new Range('A', 'Z').except('A'))
            .regex("[A-Z&&[^A]]")
            .noMatch("A")
            .matches("B", "Z")
        ;
        test(new Range('A', 'Z').except('A', 'Z'))
            .regex("[A-Z&&[^AZ]]")
            .noMatch("A", "Z")
            .matches("B", "Y")
        ;
        test(new Range('A', 'Z').except(new Range('X', 'Z')))
            .regex("[A-Z&&[^X-Z]]")
            .matches("A", "W")
            .noMatch("Y")
        ;
        test(new Range('A', 'Z').except(new Range('X', 'Z'), new Single('A')))
            .regex("[A-Z&&[^X-ZA]]")
            .matches("B", "W")
            .noMatch("A", "Y")
        ;
    }

    @Test void predefined()
    {
        test(new Predefined("\\d"))
            .matches("0", "0")
            .noMatch("A", "Z")
        ;
        test(new Predefined("\\w").except(new Predefined("\\p{Lower}")))
            .regex("[\\w&&[^\\p{Lower}]]")
            .matches("A")
            .noMatch("a")
        ;
        test(new Union(new Single('a'), new Predefined("\\p{javaUpperCase}")))
            .matches("a", "A", "B")
            .noMatch("b")
        ;
    }


}