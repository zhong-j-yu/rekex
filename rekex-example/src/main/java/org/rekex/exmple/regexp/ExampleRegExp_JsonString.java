package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import static org.rekex.regexp.RegExpApi.*;

public class ExampleRegExp_JsonString
{
    public static RegExp exp()
    {
        var digit = alt(
            range('0', '9')
        );
        var hex = alt(
            digit,
            range('A', 'F'),
            range('a', 'f')
        );
        var quote = (
            '"'
        );
        var backslash = (
            '\\'
        );
        var escape = alt(
            quote,
            backslash,
            ch("bfnrt"),
            seq('u', times(4, hex))
        );
        var character = alt(
            range(0x0020, 0x10FFFF).except(quote, backslash),
            seq(backslash, escape)
        );
        var characters = alt(
            rep0(character)
        );
        var string = alt(
            seq(quote, characters, quote)
        );
        return string;
   }

    public static void main(String[] args)
    {
        ExampleRegExpUtil.testInputs(exp(), "string, http://json.org");
    }
}