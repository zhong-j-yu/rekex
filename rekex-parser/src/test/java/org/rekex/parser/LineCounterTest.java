package org.rekex.parser;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class LineCounterTest
{
    @Test
    void test1()
    {
        check("A\r\nB\rC\nD", 0, 1, 3, 4, 5, 6, 7, 8);

        check("", 0, 0);

        check("\n\r", 0,0, 1,1, 2,2);
    }

    void check(String text, int... startAndEnds)
    {
        LineCounter lc = new LineCounter(text);
        if(false)
        {
            String arr = IntStream.range(0, lc.lines())
                .peek(l->System.out.printf("line#%d [%d %d) %n", l, lc.lineStart(l), lc.lineEnd(l)))
                .mapToObj(l->", "+lc.lineStart(l)+", "+lc.lineEnd(l))
                .collect(Collectors.joining());
            System.out.println(arr);
        }

        int lines = startAndEnds.length/2;
        assertEquals(lc.lines(), lines);
        for(int l=0; l<lines; l++)
            assertEquals(lc.lineStart(l), startAndEnds[2*l]);
        for(int l=0; l<lines; l++)
            assertEquals(lc.lineEnd(l), startAndEnds[2*l+1]);

        int L=-1, C=0;
        for(int pos=0; pos<lc.end+1; pos++)
        {
            int L2 = lc.line(pos);
            if(L2==L+1)
            {
                L++;
                C=0;
            }
            else
            {
                assertEquals(L2, L);
                C++;
            }
            //System.out.printf("%d %d %d %n", pos, lc.column(pos), C);
            assertEquals(lc.column(pos), C);
        }

    }
}