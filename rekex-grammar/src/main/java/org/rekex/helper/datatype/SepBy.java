package org.rekex.helper.datatype;

import org.rekex.spec.Ctor;
import org.rekex.spec.Permits;

import java.util.List;

/**
 * Zero or more T, separated by S.
 * See also {@link SepBy1}.
 */
//  ( T (S T)* )?
// this datatype could match empty input.
@Permits({SepBy1.class, SepBy.Empty.class})
public sealed interface SepBy<T, S>
    permits SepBy1, SepBy.Empty
{
    List<T> values();
    List<S> separators();

    record Empty<T,S>() implements SepBy<T,S>
    {
        @Ctor public static <T,S> Empty<T,S> ctor()
        {
            return new Empty<>();
        }

        @Override
        public List<T> values()
        {
            return List.of();
        }

        @Override
        public List<S> separators()
        {
            return List.of();
        }
    }
}
