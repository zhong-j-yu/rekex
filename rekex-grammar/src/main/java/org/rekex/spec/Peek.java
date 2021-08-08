package org.rekex.spec;

// in concatenation, Peek<T> has the same matching effect as Not<Not<T>>.
//    but we have this class anyway, so that (T value) can be stored.
// in conjunction, Not<> has a different meaning. Use Peek<> and Peek<Not> for peeking.

/**
 * Datatype to represent positive lookahead, a.k.a. `and-predicate` in PEG.
 */
public record Peek<T>(T value)
{
}
