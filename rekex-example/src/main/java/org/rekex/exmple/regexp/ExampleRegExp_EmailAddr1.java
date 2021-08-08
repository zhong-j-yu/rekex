package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import static org.rekex.regexp.RegExpApi.*;

// `addr-spec` from https://datatracker.ietf.org/doc/html/rfc5322#section-3.4.1
// This grammar of email address is infamous for its complexity,
// allowing whitespaces, newlines, quoted strings, escaped chars, comments, etc.
// In practice that's not how people think of a valid email address.
// But it's great for this example to demonstrate how RegExpApi handles the grammar.
//
// Saner definitions of email address:
//    `email`   from https://html.spec.whatwg.org/multipage/input.html#valid-e-mail-address
//    `Mailbox` from https://datatracker.ietf.org/doc/html/rfc5321#section-4.1.2
class ExampleRegExp_EmailAddr1
{
    public static RegExp exp()
    {
        // RFC 1035 $2.3.1
        var digit = range('0', '9');
        var letter = alt(range('A', 'Z'), range('a', 'z'));
        var let_dig = alt(letter, digit);
        var let_dig_hyp = alt(let_dig, '-');
        var ldh_str = rep1(let_dig_hyp);
        var label = seq(
            letter,
            opt(
                opt(ldh_str),
                let_dig)
        );
        var subdomain = seq(label, rep0('.', label));
        var domain = subdomain; // rfc allows " " to a domain too? ignore that.

        // RFC 5234 appendix-B

        var CR = 0x0D;
        var LF = 0x0A;
        var CRLF = seq(CR, LF);
        var SP = 0x20;
        var HTAB = 0x09;
        var WSP = alt(SP, HTAB);
        var VCHAR = range(0x21, 0x7E);

            // RFC 5322 $3.4.1

        var quoted_pair = seq( "\\", alt(VCHAR, WSP) );

        var FWS = seq(
            opt(
                rep0(WSP),
                CRLF),
            rep1(WSP)
        );
        var ctext = range(33, 126).except('(', ')', '\\');

        // RFC allows nested comment, which requires recursion, which we can't support.
        var ccontent = alt(ctext, quoted_pair);
        var comment = seq("(", rep0(opt(FWS), ccontent), opt(FWS), ")");

        var CFWS = alt(
            seq(
                rep1(opt(FWS), comment),
                opt(FWS)),
            FWS
        );

        var DQUOTE = 0x22;

        var qtext = range(33, 126).except(DQUOTE, '\\');
        var qcontent = alt(qtext, quoted_pair);
        var quoted_string = seq(
            opt(CFWS), DQUOTE,
            rep0(opt(FWS), qcontent),
            opt(FWS), DQUOTE, opt(CFWS)
        );

        var alpha = letter;
        var atext = alt(alpha, digit, ch("!#$%&'*+-/=?^_`{|}~"));
        var dot_atom_text = seq(
            rep1(atext),
            rep0(".", rep1(atext))
        );
        var dot_atom = seq(opt(CFWS), dot_atom_text, opt(CFWS));


        var local_part = alt(dot_atom, quoted_string);

        var addr_spec = seq(local_part, "@", domain);

        return addr_spec;
    }

    public static void main(String[] args)
    {
        ExampleRegExpUtil.testInputs(exp(), "email addr_spec, https://datatracker.ietf.org/doc/html/rfc5322#section-3.4.1");
    }

}