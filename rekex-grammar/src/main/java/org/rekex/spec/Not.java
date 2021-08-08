package org.rekex.spec;

// concatenation(..Not<B>..)
//     tests that the input from that position does not match B.
//     if success, does not consume input. same effect as Peek<Not<B>>.
//     Not<Not<B>> same matching effect as Peek<B> (except without storing the value of B)
//         reason: in the case of success, any length of input does not match B,
//         so how much should Not<?> consume? only 2 sensible choices: 0 or all.
//         consider Not<Not<B>> on an input that does start with B.
//            If Not<?> consumes all, Not<Not<B>> succeeds and consume all input, for matching B, which is odd.
//            If Not<?> consumes 0, Not<Not<B>> consumes 0 as well, which is self-consistent, as in PEG.
//     Do not recommend bare Not<> in concatenation; always use Peek<Not<B>> or Peek<B>.
//         Peek<Not<B>> is clear that it's a lookahead.
//         Not<B> by itself may sound like it matches and consumes something that's not B.
//     Not<AnyChar> - assert EOF is reached

/**
 * Datatype to represent negative lookahead, a.k.a. `not-predicate` in PEG.
 */
public record Not<T>
    ( /* empty */ )
{
}
