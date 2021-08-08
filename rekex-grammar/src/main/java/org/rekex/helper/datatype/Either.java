package org.rekex.helper.datatype;

import org.rekex.spec.Permits;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Either L or R, as a tagged union type.
 */
@Permits({Either.Left.class, Either.Right.class})
public sealed interface Either<L, R>
{
    /**
     * Map the value with either `mapL` or `mapR`, depending on the type of `this`.
     */
    <T> T caseOf(Function<L,T> mapL, Function<R,T> mapR);

    record Left<L, R>
        (L value)
    implements Either<L, R>
    {
        @Override
        public <T> T caseOf(Function<L, T> mapL, Function<R, T> mapR)
        {
            return mapL.apply(value);
        }
    }

    record Right<L, R>
        (R value)
    implements Either<L, R>
    {
        @Override
        public <T> T caseOf(Function<L, T> mapL, Function<R, T> mapR)
        {
            return mapR.apply(value);
        }
    }

}
