package org.rekex.parser;

import org.rekex.spec.Ctor;

import java.util.Arrays;

/**
 * A special datatype that retains input information corresponding to a matched rule.
 */
public class ParseInfo
{
    // Syntactically this datatype matches epsilon, so it can be inserted anywhere.
    // The grammar builder sees it as a normal datatype.
    // The parser treats it specially.
    @Ctor public static ParseInfo ctor()
    {
        throw new AssertionError("should not be invoked");
    }

    final InputInfo input;

    // this datatype can be inserted anywhere in any ctor with N arguments;
    // N+1 positions are retained for this node and direct sub nodes.
    // later we could also add the stack of parent symbols and start positions.
    final int argCount;
    final Object[] args;
    final int[] positions;

    public ParseInfo(InputInfo input, Object[] args, int[] positions)
    {
        this.input = input;

        assert positions.length == args.length+1;
        this.argCount = args.length;
        this.args = args;
        this.positions = positions;
    }

    /**
     * The input information.
     */
    public InputInfo input()
    {
        return input;
    }

    private LineCounter lc(){ return input.lineCounter(); }

    /**
     * The total number of arguments.
     */
    public int argCount()
    {
        return argCount;
    }
    /**
     * The argument at `argIndex`, starting from 0.
     */
    public Object arg(int argIndex)
    {
        return args[argIndex];
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
        return lc().line(start());
    }
    /**
     * The column number of the start position
     */
    public int startColumn()
    {
        return lc().column(start());
    }

    /**
     * The end position where the rule is matched.
     */
    public int end()
    {
        return positions[argCount];
    }

    /**
     * The line number of the end position
     */
    public int endLine()
    {
        return lc().line(end());
    }
    /**
     * The column number of the end position
     */
    public int endColumn()
    {
        return lc().column(end());
    }

    /**
     * The text for which the rule is matched.
     */
    public CharSequence text()
    {
        return input.chars.subSequence(start(), end());
    }

    /**
     * Info for a particular argument for the ctor.
     */
    public ParseInfo of(Object arg)
    {
        int index = -1;
        for(int i = 0; i< argCount; i++)
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

        return subInfo(index);
    }

    /**
     * Info for a particular argument for the ctor,
     * based on its index in the argument list, starting from 0.
     */
    public ParseInfo ofIndex(int argIndex)
    {
        if(argIndex<0 || argIndex>= argCount)
            throw new IllegalArgumentException("invalid argIndex: "+argIndex);
        return subInfo(argIndex);
    }

    private ParseInfo subInfo(int index)
    {
        return new ParseInfo(input, new Object[]{args[index]},
            new int[]{positions[index], positions[index+1]});
    }

    public String toString()
    {
        return Arrays.toString(positions);
    }
}
