package org.rekex.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

// text :=  (content linebreak)* content <EOF>
// content: 0 or more chars not containing linebreak
// linebreak: user specified, default to: CR LF | CR | LF
//
// note that there is at least one line, even for empty text.
// lines and columns are 0-based; position 0 = line 0 col 0
// position at <EOF> is valid, considered at the last line.

/**
 * To convert a position to line and column number.
 */
public class LineCounter
{
    final int start;
    final int end;
    final int[] lineStarts;
    final int[] lineEnds; // does not include the linebreak

    public LineCounter(CharSequence chars)
    {
        this(defaultLinebreak(), chars, 0, chars.length());
    }

    /**
     * The default line break, which is `CR LF | CR | LF`
     */
    public static Pattern defaultLinebreak()
    {
        return Pattern.compile("\\r\\n|\\r|\\n");  // user may want "\\R" instead
    }

    public LineCounter(Pattern linebreak, CharSequence chars, int start, int end)
    {
        ArrayList<Integer> starts = new ArrayList<>();
        ArrayList<Integer> ends = new ArrayList<>();

        var matcher = linebreak.matcher(chars).region(start, end);

        starts.add(start);
        while(matcher.find())
        {
            ends.add(matcher.start());
            starts.add(matcher.end());
        }
        ends.add(end);

        this.start = start;
        this.end = end;
        this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
        this.lineEnds = ends.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * The line number for the position.
     * The number for the first line is `0`.
     */
    public int line(int pos)
    {
        if(!(start<=pos && pos<=end)) // allow pos at EOF
            throw new IllegalArgumentException("pos %d not in range[%d,%d]".formatted(pos, start, end));
        int x = Arrays.binarySearch(lineStarts, pos);
        if(x<0) x = -x-2;
        assert 0<=x && x<lineStarts.length;
        return x;
    }

    /**
     * The column number for the position within the line.
     * The number for the first column is `0`.
     */
    public int column(int pos)
    {
        int x = line(pos);
        return pos-lineStarts[x];
    }

    /**
     * Total number of lines.
     */
    public int lines()
    {
        return lineStarts.length;
    }

    /**
     * The start position of a line.
     */
    public int lineStart(int line)
    {
        return lineStarts[line];
    }
    /**
     * The end position of a line.
     */
    public int lineEnd(int line)
    {
        return lineEnds[line];
    }

}