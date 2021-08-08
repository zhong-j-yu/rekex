package org.rekex.grammar;

import org.rekex.annotype.AnnoType;

public record NegRule(
    Integer id,
    AnnoType datatype,
    Integer subRuleId
)
    implements GrammarRule
{

}
