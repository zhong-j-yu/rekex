package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import static org.rekex.regexp.RegExpApi.*;

public class ExampleRegExp_JsonNumber
{
    // defined exactly as json.org's grammar
    public static RegExp exp0()
    {
        var one_nine = alt(
            range('1', '9')
        );
        var digit = alt(
            '0',
            one_nine
        );
        var digits = alt(
            rep1(digit)
        );
        var integer = alt(
            digit,
            seq(one_nine, digits),
            seq('-', digit),
            seq('-', one_nine, digits)
        );
        var fraction = alt(
            "",
            seq('.', digits)
        );
        var sign = alt(
            "",
            '+',
            '-'
        );
        var exponent = alt(
            "",
            seq('E', sign, digits),
            seq('e', sign, digits)
        );
        var number = alt(
            seq(integer, fraction, exponent)
        );

        return number;
    }

    // defined more naturally
    public static RegExp exp()
    {
        var digit =
            range('0', '9')
            ;
        var digits =
            rep1(digit)
            ;
        var unsignedM =
            seq(range('1', '9'), digits)
            ;
        var unsigned =
            alt(digit, unsignedM)
            ;
        var integer =
            seq(opt('-'), unsigned)
            ;
        var fraction =
            seq('.', digits)
            ;
        var exponent =
            seq(ch("Ee"), opt(ch("+-")), digits)
            ;
        var number =
            seq(integer, opt(fraction), opt(exponent))
            ;

        return number;
    }

    public static void main(String[] args)
    {
        ExampleRegExpUtil.testInputs(exp(), "number, http://json.org");
    }
}