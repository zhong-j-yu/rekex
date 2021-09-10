package org.rekex.parser;

import org.rekex.spec.Ctor;

import java.util.Arrays;

/**
 * A special datatype that retains input information corresponding to a matched rule.
 */
public class ParseInfo
{
    // syntactically this datatype matches epsilon;
    // the grammar builder sees it as a normal datatype.
    // It is the specific parser that handles it specially.
    @Ctor public static ParseInfo ctor()
    {
        throw new AssertionError("should not be invoked");
    }

    // this datatype can be inserted anywhere in any ctor with N arguments;
    // N+1 positions are retained.
    final CharSequence input;
    final LineCounter lc;
    final int N;
    final Object[] args;
    final int[] positions;

    public ParseInfo(CharSequence input, LineCounter lc, Object[] args, int[] positions)
    {
        this.input = input;
        this.lc = lc;

        assert positions.length == args.length+1;
        this.N = args.length;
        this.args = args;
        this.positions = positions;
    }

    /**
     * The whole original input
     */
    public CharSequence input()
    {
        return input;
    }

    public LineCounter lineCounter()
    {
        return lc;
    }

    /**
     * The start position where the rule is matched.
     */
    public int start()
    {
        return positions[0];
    }

    /**
     * The line number of the start position
     */
    public int startLine()
    {
        return lc.line(start());
    }
    /**
     * The column number of the start position
     */
    public int startColumn()
    {
        return lc.column(start());
    }

    /**
     * The end position where the rule is matched.
     */
    public int end()
    {
        return positions[N];
    }

    /**
     * The line number of the end position
     */
    public int endLine()
    {
        return lc.line(end());
    }
    /**
     * The column number of the end position
     */
    public int endColumn()
    {
        return lc.column(end());
    }

    /**
     * The text for which the rule is matched.
     */
    public CharSequence text()
    {
        return input.subSequence(start(), end());
    }

    /**
     * Info for a particular argument for the ctor.
     */
    public ParseInfo of(Object arg)
    {
        int index = -1;
        for(int i=0; i<N; i++)
        {
            if(args[i]==arg) // by obj identity. does not work for primitives.
            {
                if(index!=-1)
                    throw new RuntimeException("two args [#%d, #%d] matched arg %s"
                        .formatted(index, i, arg));
                index = i;
            }
        }
        if(index==-1)
            throw new RuntimeException("zero args matched arg %s".formatted(arg));

        return new ParseInfo(input, lc, new Object[]{arg}, new int[]{positions[index], positions[index+1]});
    }

    public String toString()
    {
        return Arrays.toString(positions);
    }
}
