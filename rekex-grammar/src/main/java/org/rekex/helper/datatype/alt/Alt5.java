package org.rekex.helper.datatype.alt;

import org.rekex.spec.Permits;

/**
 * Variant of 5 types.
 */
@Permits({Alt5.Choice1.class, Alt5.Choice2.class, Alt5.Choice3.class, Alt5.Choice4.class, Alt5.Choice5.class})
public sealed interface Alt5<T1, T2, T3, T4, T5>
{
    Object value();

    record Choice1<T1, T2, T3, T4, T5> (T1 value) implements Alt5<T1, T2, T3, T4, T5>
    {}

    record Choice2<T1, T2, T3, T4, T5> (T2 value) implements Alt5<T1, T2, T3, T4, T5>
    {}

    record Choice3<T1, T2, T3, T4, T5> (T3 value) implements Alt5<T1, T2, T3, T4, T5>
    {}

    record Choice4<T1, T2, T3, T4, T5> (T4 value) implements Alt5<T1, T2, T3, T4, T5>
    {}

    record Choice5<T1, T2, T3, T4, T5> (T5 value) implements Alt5<T1, T2, T3, T4, T5>
    {}

}
