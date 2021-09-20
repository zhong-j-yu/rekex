package org.rekex.parser;

public class InputInfo
{
    final CharSequence chars;
    final int start;
    final int end;

    public InputInfo(CharSequence chars, int start, int end)
    {
        this.chars = chars;
        this.start = start;
        this.end = end;
    }

    public CharSequence chars()
    {
        return chars;
    }

    public int start()
    {
        return start;
    }

    public int end()
    {
        return end;
    }

    private LineCounter lineCounter = null;
    // the default linebreak is used;
    // if needed, we can make it an option in ParserBuilder.

    public LineCounter lineCounter()
    {
        var v = lineCounter;
        if(v==null)
            lineCounter = v = new LineCounter(LineCounter.defaultLinebreak(), chars, start, end);
        return v;
    }

}
