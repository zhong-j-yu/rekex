package org.rekex.helper.datatype.alt;

import org.rekex.spec.Permits;

/**
 * Variant of 3 types.
 */
@Permits({Alt3.Choice1.class, Alt3.Choice2.class, Alt3.Choice3.class})
public sealed interface Alt3<T1, T2, T3>
{
    record Choice1<T1, T2, T3> (T1 value) implements Alt3<T1, T2, T3>{}

    record Choice2<T1, T2, T3> (T2 value) implements Alt3<T1, T2, T3>{}

    record Choice3<T1, T2, T3> (T3 value) implements Alt3<T1, T2, T3>{}
}
