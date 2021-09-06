package org.rekex.helper.datatype.alt;

import org.rekex.spec.Permits;

/**
 * Variant of 6 types.
 */
@Permits({Alt6.Choice1.class, Alt6.Choice2.class, Alt6.Choice3.class, Alt6.Choice4.class, Alt6.Choice5.class, Alt6.Choice6.class})
public sealed interface Alt6<T1, T2, T3, T4, T5, T6>
{
    Object value();

    record Choice1<T1, T2, T3, T4, T5, T6> (T1 value) implements Alt6<T1, T2, T3, T4, T5, T6>
    {}

    record Choice2<T1, T2, T3, T4, T5, T6> (T2 value) implements Alt6<T1, T2, T3, T4, T5, T6>
    {}

    record Choice3<T1, T2, T3, T4, T5, T6> (T3 value) implements Alt6<T1, T2, T3, T4, T5, T6>
    {}

    record Choice4<T1, T2, T3, T4, T5, T6> (T4 value) implements Alt6<T1, T2, T3, T4, T5, T6>
    {}

    record Choice5<T1, T2, T3, T4, T5, T6> (T5 value) implements Alt6<T1, T2, T3, T4, T5, T6>
    {}

    record Choice6<T1, T2, T3, T4, T5, T6> (T6 value) implements Alt6<T1, T2, T3, T4, T5, T6>
    {}

}
