package org.rekex.grammar;

import org.rekex.annotype.AnnoType;

// 0<=min<=max<=Int.MAX.
// (0,n) - always match. worst case it matches with 0 items
//         (0,0) - matches epsilon.
// (1,n) - one or more
//         (1,1) - match exactly one. weird, but allowed.
public record RepeatRule(
    Integer id,
    AnnoType datatype,
    Integer subRuleId,
    int min,
    int max // inclusive
)
    implements GrammarRule
{

}
