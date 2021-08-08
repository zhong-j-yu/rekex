package org.rekex.helper.datatype.seq;

// match concatenation of 2 rules.
// Also a 2-tuple datatype.
// We provide a few simple SeqX as examples;
//   users can easily create their own if they need more features.
// Arguably, Seq3 is a subtype of Seq2, and so on
//     Seq3<T1,T2, T3> <: Seq2<T1,T2>
//   we don't bother because it's not that useful in practice.

/**
 * A sequence of 2 values.
 */
public record Seq2<T1, T2>
    (T1 v1, T2 v2)
{
}
