package org.rekex.grammar;

import org.rekex.annotype.AnnoType;

import java.util.List;

// arity:
//   0: epsilon. always match. e.g. Opt.None
//   1: rename symbol. convert datatype.
//   2: concat of symbols

public record ConcatRule(
    Integer id,
    AnnoType datatype,
    List<Integer> subRuleIds,
    Instantiator instantiator
) implements GrammarRule
{

}
