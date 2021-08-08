package org.rekex.grammar;

import org.rekex.annotype.AnnoType;
import org.rekex.spec.Regex;

// instantiator
//     could be null if datatype is string/char
//     if non-null, args are (CharSequence, start, end)
public record RegexRule(
    Integer id,
    AnnoType datatype,
    Regex regex,
    Instantiator instantiator
)
    implements GrammarRule
{

}
