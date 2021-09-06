package org.rekex.helper.datatype.alt;

import org.rekex.spec.Permits;

// We provide a few simple AltX as examples;
//   users can easily create their own if they need more features.
// There could be subtyping relationship among AltN
//   but it's a little tricky and complicated, and not much useful.

/**
 * Variant of 2 types.
 * See also {@link org.rekex.helper.datatype.Either Either}
 */
@Permits({Alt2.Choice1.class, Alt2.Choice2.class})
public sealed interface Alt2<T1, T2>
{
    Object value();

    record Choice1<T1, T2> (T1 value) implements Alt2<T1, T2>{}

    record Choice2<T1, T2> (T2 value) implements Alt2<T1, T2>{}

}
