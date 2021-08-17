package org.rekex.parser;

/**
 * A PEG parser for root type `T`.
 * <p>
 *     The parser is stateless and thread-safe;
 *     it can be invoked multiple times for different inputs.
 * </p>
 */

public interface PegParser<T>
{
    /**
     * Create a parser with the root type as the `rootClass`.
     * For more options, see {@link PegParserBuilder}.
     */
    public static <T> PegParser<T> of(Class<T> rootClass)
    {
        return new PegParserBuilder()
            .rootType(rootClass)
            .parser();
    }

    /**
     * Equivalent to `parse(chars, 0. chars.length())`
     */
    default ParseResult<T> parse(CharSequence chars)
    {
        return parse(chars, 0, chars.length());
    }

    /**
     * Parse the input `chars` from `start` to `end` (exclusive).
     */
    ParseResult<T> parse(CharSequence chars, int start, int end);

    /**
     * Parse the input; if the result is {@link ParseResult.Full},
     * return the value of `T`; otherwise throw an exception.
     *
     */
    default T matchFull(CharSequence chars) throws Exception
    {
        ParseResult<T> result = parse(chars);
        if(result instanceof ParseResult.Full<T> all)
            return all.value();
        if(result instanceof ParseResult.Partial<T> partial)
            throw new Exception("matched partially; position="+partial.position());
        if(result instanceof ParseResult.Fail<T> fail)
            throw new Exception("failed to match: "+fail.toString());
        if(result instanceof ParseResult.Fatal<T> fatal)
            throw new Exception("fatal error: "+fatal.toString(), fatal.cause());
        throw new AssertionError();
    }

}
