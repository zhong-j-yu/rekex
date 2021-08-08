package org.rekex.grammar;

import org.rekex.annotype.AnnoType;

import java.util.List;

// the first successful choice creates the T value.
// all subrules have datatypes assignable to this datatype
// arity:
//   0: never match.
//   1: equivalent to the subRule.
//   2. multiple choices.
// sub rules are ordered.

public record AltRule(
    Integer id,
    AnnoType datatype,
    List<Integer> subRuleIds
)
    implements GrammarRule
{

}
