package org.rekex.exmple.regexp;

import java.util.regex.Pattern;

import static org.rekex.regexp.RegExpApi.*;

// codes referenced by the user's guide
@SuppressWarnings("all")
class ExampleRegExp_UserGuide
{
    static void time()
    {
        // time format HH:MM:SS, optionally with .mmm

        var digit = range('0', '9');   // 0-9
        var HH = alt(
            seq(alt('0','1'), digit),  // 00-19
            seq('2', ch("0123")),     // 20-23
            "24"                       // sake of example
        );
        var MM = seq(range('0', '5'), digit); // 00-59
        var SS = MM;
        var mmm = times(3, digit);    // 000-999

        var time = seq(HH, ":", MM, ":", SS, opt(".", mmm));

        System.out.println( toTreeText(time) );
        System.out.println( toRegex(time) );

        time = simplify(time);

        System.out.println( toTreeText(time) );

        String regex = toRegex(time);
        // (?:[01][0-9]|2[0123]|24):[0-5][0-9]:[0-5][0-9](?:\.[0-9]{3})?

        boolean match = Pattern.matches(regex, "12:34:56.789");

        System.out.println(regex);
        System.out.println(match);
    }

    static void doc()
    {
        // a single char is a regex

        var CR = '\r';
        var LF = 0x0A; // int as char

        // sequence

        var CRLF = seq(CR, LF);

        var TRUE = seq("true"); // seq('t', 'r', 'u', 'e')

        var taut = seq(TRUE, '=', "true");

        var epsilon = seq(); // or seq("")

        // alt

        var bool = alt(
            "true",
            "false"
        );

        var abc = alt(
            'a',
            'b',
            'c'
        );

        var abc_ = ch("abc"); // alt('a','b','c')

        var digit = range('0', '9');

        // except

        var ab1_9 = alt(abc, digit).except('c', '0');

        var opt_bool = alt(
            "true",
            "false",
            ""
        );

        boolean enable24 = false;
        var HH = alt(
            seq(ch("01"), digit),
            seq('2', ch("0123")),
            (enable24 ? "24" : alt())
        );

        var PLUS = alt(
            '+'
        );
        var MINUS = alt(
            '-'
        );
        var sign = alt(
            PLUS,
            MINUS
        );


        var opt_sign = opt(sign);  // optionally + or -
        var digits0 = rep0(digit); // 0 or more digits
        var digits1 = rep1(digit); // 1 or more digits

        var d4     = times(4, digit);     // 4 digits
        var d4_8   = times(4, 8, digit);  // 4 to 8 digits
        var d4_INF = times(4, Long.MAX_VALUE, digit); // 4 or more

        var t2 = seq(digit, opt(sign, digit)); // d, d+d, d-d

        var rd = reluctant(digits0);
        var pd = possessive(digits0);


        // named group

        var g1 = group("g1", HH);
        var ref1  = backRef("g1");
        var ref1_ = backRef(g1);

        var MM = seq(range('0', '5'), digit);
        var g2 = group(MM);
        var ref2 = backRef(2);

        var exp = seq(g1, ":", g2, ",", ref2);
        var gn2 = findGroupNumberIn(g2, exp);

        // atomic group

        var digits_all = atomicGroup(rep0(digit));


        // look ahead/behind, boundary

        var notLedByZero = lookahead(false, seq("0", digit));
        var number = seq(notLedByZero, rep1(digit));
        // does not match if "0" is followed by more digits

        var EOF = boundary_end_of_input();


        // flag

        var abc_ci = flag(true, Pattern.CASE_INSENSITIVE, abc);
        var abc_ci2 = ignoreCase(abc);


        // opaque

        var unicode_linebreak = opaque("\\R");

        var unicode_cluster_boundary = boundary("\\b{g}");

        var wordChar = predefined("\\w");
        var UPPER = predefined("\\p{Upper}");
        var beta = predefined("\\N{GREEK SMALL LETTER BETA}");


        // char class

        var xyz = union('x', 'y', 'z');
        var xyz2 = alt('x', 'y', 'z');
        var xyz3 = ch("xyz");
        var xyz4 = range('x', 'y');

        var intersect = intersect(ch("abc"), ch("bcd")); // {'b','c'}

        var all = intersect();
        var all2 = range(0, 0x10FFFF);

        var nonDigit = negate(digit); // any char except 0-9
        var nonDigit2 = all2.except(digit);

        var ASCII = range(0x20, 0x7E);
        var nonDigitAscii = intersect(ASCII, negate(digit));
        var nonDigitAscii2 = ASCII.except(digit);

        String regex = toRegex(exp);

        System.out.println( toTreeText(exp) );

        exp = simplify(exp);
    }

}
