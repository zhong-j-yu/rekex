package org.rekex.regexp;

import org.junit.jupiter.api.Test;

import static org.rekex.regexp.RegExp.*;
import static org.rekex.regexp.RegExpApi.*;

import static org.junit.jupiter.api.Assertions.*;

class ToSimplifyTest extends RegExpTestBase
{
    @Test
    void testQuant()
    {
        long INF = Long.MAX_VALUE;
        testQuant(2,3,4,5, 2*4, 3*5);

        testQuant(0, 0, 1, 2, 0, 0);
        testQuant(0, 0, 1, INF, 0, 0);

        testQuant(1, 2, 1, INF, 1, INF);

        testQuant(1, INF-1, 1, INF-2, 1, INF-2); // max1*max2 overflow
        
    }
    void testQuant(int min1, long max1, int min2, long max2, long minE, long maxE)
    {
        var q1 = times(min1, max1, seq("A"));
        var q2 = times(min2, max2, q1);
        q2 = (Quantified) simplify(q2);
        assertEquals(minE, q2.min());
        assertEquals(maxE, q2.max());
    }


    @Test
    void testAltToCc()
    {
        var alt = alt(
            "A", "B",
            "xx",
            "A", union(), "B", range('0','9'), ch("OPQ"), ch("XYZ").except('Y'),
            "yy",
            "C", "D"
        );
        test(alt)
            .regex("A|B|xx|A|[0&&1]|B|[0-9]|[OPQ]|[XYZ&&[^Y]]|yy|C|D");
        test(simplify(alt))
            .regex("[AB]|xx|[AB0-9OPQ[XYZ&&[^Y]]]|yy|[CD]");
    }
}