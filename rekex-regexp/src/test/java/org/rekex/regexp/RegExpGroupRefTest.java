package org.rekex.regexp;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.rekex.regexp.RegExp.*;
import static org.rekex.regexp.RegExpApi.*;

class RegExpGroupRefTest extends RegExpTestBase
{

    @Test void testAtomicGroup()
    {
        var ag = atomicGroup(alt("AB", "A"));
        var r = seq(ag, "B");
        test(r)
            .dump()
            .matches("ABB")
            .noMatch("AB");
    }

    @Test void testGroup()
    {
        test(group(seq()))
            .regex("()")
            ;
        test(group("g1", seq()))
            .regex("(?<g1>)")
            ;

        var L = range('A', 'Z');
        var r = seq(L, group(L), L, group("g2", L), L);
        var m = Pattern.compile(toRegex(r)).matcher("ABCDE");
        assertTrue(m.matches());
        assertEquals(m.group(1), "B");
        assertEquals(m.group("g2"), "D");
    }

    @Test void testBackRef()
    {
        {
            var g1 = group("g1", range('A', 'Z'));
            var r = seq(g1, range('0', '9'), backRef(g1));
            test(r)
                .regex("(?<g1>[A-Z])[0-9]\\k<g1>")
                .matches("A2A")
                .noMatch("A2B");
        }

        // named/numbered back-ref to g inside g is allowed
        test(group("g3", backRef("g3")))
            .regex("(?<g3>\\k<g3>)")
        ;
        test(group(backRef(1)))
            .regex("(\\1)")
        ;
        // because it's possible that g matched without the back-ref.
        test(rep0(group((alt("A", seq("B", backRef(1)))))))
            .matches("ABA");
    }

    @Test void testSickBackRef()
    {
        // unnamed group, shared in tree. ok.
        {
            var g = group(seq("A"));
            test(seq(g, g))
                .regex("(A)(A)")
                .matches("AA")
            ;
        }

        // back-ref to group 0.
        assertThrows(IllegalArgumentException.class, ()->backRef(0));

        // groups with the same name
        {
            var g = group("g1", seq());
            assertThrows(PatternSyntaxException.class, ()->{
                test(seq(g, g));
            });
        }

        {
            var g = group("g1", seq("A"));
            // named back-ref to non-existing name
            assertThrows(PatternSyntaxException.class, ()->{
                test(seq(g, backRef("g2")));
            });
            // named back-ref to g before g
            assertThrows(PatternSyntaxException.class, ()->{
                test(seq(backRef("g1"), g));
            });

            // but... numbered back-refs are not checked against such errors.

            // ref to non-existing group
            test(seq(g, backRef(2)))
                .regex("(?<g1>A)\\2")
            ;
            // ref to g before g
            test(seq(backRef(1), g))
                .regex("\\1(?<g1>A)")
            ;

        }
    }

    @Test void testCountGroup()
    {
        {
            //   ( () () ) ( () () )
            //   1 2  3    4 5  6

            Group[] g = new Group[7];
            for(int i=0; i<g.length; i++)
                g[i] = group(seq(""+i));
            g[1] = group(seq(g[2], g[3]));
            g[4] = group(seq(g[5], g[6]));
            var r = seq(g[1], g[4]);
            for(int i=1; i<=6; i++)
                assertEquals(i, findGroupNumberIn(g[i], r));

            assertEquals(-1, findGroupNumberIn(g[0], r));
        }


        // count through opaque
        {
            var g = group(alt('B'));
            var r = seq(opaque("(A)"), g);
            assertEquals(2, findGroupNumberIn(g, r));
        }

        // shared group, multiple nodes in tree. return first match
        {
            var g = group(seq("A"));
            var r = seq(g, g);
            assertEquals(1, findGroupNumberIn(g, r));
        }

    }

}