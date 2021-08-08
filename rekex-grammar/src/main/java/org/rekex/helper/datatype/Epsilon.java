package org.rekex.helper.datatype;

// always matches any input

import org.rekex.spec.Ctor;

/**
 * Epsilon matches an empty input.
 */
public record Epsilon()
{
    @Ctor public static Epsilon ctor(/* empty */)
    {
        return new Epsilon();
    }
}
