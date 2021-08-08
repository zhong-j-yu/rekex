package org.rekex.helper.datatype.alt;

import org.rekex.spec.Permits;

/**
 * Variant of 4 types.
 */
@Permits({Alt4.Choice1.class, Alt4.Choice2.class, Alt4.Choice3.class, Alt4.Choice4.class})
public sealed interface Alt4<T1, T2, T3, T4>
{
    record Choice1<T1, T2, T3, T4> (T1 value) implements Alt4<T1, T2, T3, T4>{}

    record Choice2<T1, T2, T3, T4> (T2 value) implements Alt4<T1, T2, T3, T4>{}

    record Choice3<T1, T2, T3, T4> (T3 value) implements Alt4<T1, T2, T3, T4>{}

    record Choice4<T1, T2, T3, T4> (T4 value) implements Alt4<T1, T2, T3, T4>{}

}
