package org.rekex.parser;

import org.rekex.annotype.AnnoType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The result of PegParser.parse(chars, start, end).
 */
public sealed interface ParseResult<T>
{
    /**
     * The input is matched from `start` to `end`.
     */
    record Full<T>(T value) implements ParseResult<T>
    {

    }

    /**
     * The input is matched from `start` to `position`, where position &lt; end.
     * The remaining `end-position` chars are not parsed.
     */
    record Partial<T>(T value, int position) implements ParseResult<T>
    {

    }

    // we return the farthest position in the input that the parser has reached.
    // it's likely (but not certainly) that the input can be corrected from the position forward
    // such that the corrected input matches the grammar.
    // our fail position is the maximum of such positions that we know of.
    // it is likely the actual position where the user made a mistake.
    //
    // the stack is the path of rules when the parser reached the position and failed.
    // top of the stack contains the most specific rule that failed the matching.
    // bottom of the stack is the root type of the grammar. for example,
    // (r0, p0) -> (r1, p1) -> (r2, p2),  r2 failed at `position` (usually position=p2)
    // subIndex: the 0-based index of this rule as a subrule in the parent rule (concat/alt/repeat)

    /**
     * The input failed to match the grammar.
     * The result contains a position in the input where the failure occurred,
     * and the parse stack at the time of the failure (see {@link Node}).
     */
    record Fail<T>(int position, String message, List<Node> stack) implements ParseResult<T>
    {
        @Override
        public String toString()
        {
            return "Failed at pos="+ position + ", msg="+message +
                stack.stream().map(Node::toStr).collect(Collectors.joining());
        }
    }

    /**
     * The parser stopped with a fatal error.
     * The result contains a position in the input where the error occurred,
     * and the parse stack at the time of the error (see {@link Node}).
     */
    record Fatal<T>(int position, Exception cause, List<Node> stack) implements ParseResult<T>
    {
        @Override
        public String toString()
        {
            return "Fatal at pos="+ position + ", cause="+cause.toString() +
                stack.stream().map(Node::toStr).collect(Collectors.joining());
        }
    }

    /**
     * A node in a parse stack.
     * <p>
     *     At any moment during a parsing process,
     *     there is a parse stack of rules that's being tested against the input;
     *     the bottom of the stack is the root rule;
     *     the next is a subrule of the root rule, and so on;
     *     the top of the stack is the most specific rule that is being tested.
     * </p>
     * <p>
     *     Each node in the stack contains the datatype of the rule,
     *     the start position from where the rule is being tested,
     *     the index of this rule as a subrule of the parent rule.
     * </p>
     */
    record Node(AnnoType type, int start, int subIndex)
    {
        String toStr()
        {
            String str = "\n  start="+start+", type="+type.toString(false);
            if(subIndex>=0)
                str += ", subIndex="+subIndex;
            return str;
        }
    }
}
