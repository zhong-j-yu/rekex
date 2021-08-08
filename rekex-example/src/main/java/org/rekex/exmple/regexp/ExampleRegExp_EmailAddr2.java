package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import static org.rekex.regexp.RegExpApi.*;

public class ExampleRegExp_EmailAddr2
{
    public static RegExp exp()
    {
        // https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
        var digit = range('0', '9');
        var letter = alt(range('A', 'Z'), range('a', 'z'));
        var let_dig = alt(letter, digit);
        var let_dig_hyp = alt(let_dig, '-');
        var ldh_str = rep1(let_dig_hyp);
        var label = seq(
            let_dig,
            opt(
                opt(ldh_str),
                let_dig)
        );

        var alpha = letter;
        var atext = alt(alpha, digit, ch("!#$%&'*+-/=?^_`{|}~"));

        var email = seq(
            rep1(alt(atext, '.')),
            "@",
            label,
            rep0(".", label)
        );
        return email;
    }

    public static void main(String[] args)
    {
        ExampleRegExpUtil.testInputs(exp(), "email, as define in https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address" );
    }
}