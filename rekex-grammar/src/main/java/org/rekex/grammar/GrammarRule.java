package org.rekex.grammar;

import org.rekex.annotype.AnnoType;

public sealed interface GrammarRule permits
    AltRule,
    ConcatRule,
    RepeatRule,
    PeekRule,
    NegRule,
    RegexRule
{
    Integer id();

    AnnoType datatype();

}
