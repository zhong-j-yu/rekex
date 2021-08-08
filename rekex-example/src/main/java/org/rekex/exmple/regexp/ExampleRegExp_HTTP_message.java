package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import static org.rekex.regexp.RegExpApi.*;

// rfc7230
public class ExampleRegExp_HTTP_message
{
    public static RegExp exp()
    {
        // rfc2234
        var ALPHA = alt(range(0x41, 0x5A), range(0x61, 0x7A));
        var DIGIT = range(0x30, 0x39);

        var HEXDIG = alt(DIGIT, range('A','F'), range('a','f'));
        // no lower case a-f allowed in rfc2234; but allowed by rfc3986
        // https://datatracker.ietf.org/doc/html/rfc3986#section-6.2.2.1

        // rfc3986

        var pct_encoded = seq("%", HEXDIG, HEXDIG);

        var gen_delims = ch(":/?#[]@");
        var sub_delims = ch("!$&'()*+,;=");
        var reserved = alt(gen_delims, sub_delims);
        var unreserved = alt(ALPHA, DIGIT, ch("-._~"));

        var scheme = seq(ALPHA, rep0( alt(ALPHA, DIGIT, '+', '-', '.') ));

        var userinfo = rep0(alt(unreserved, pct_encoded, sub_delims, ":"));
        var reg_name = rep0(alt(unreserved, pct_encoded, sub_delims));
        var host = alt(reg_name); // IP addresses are omitted here
        var port = rep0(DIGIT);
        var authority = seq(opt(userinfo, "@"), host, opt(":", port));

        var pchar = alt(unreserved, pct_encoded, sub_delims, ":", "@");
        var segment = rep0(pchar);
        var segment_nz = rep1(pchar);
        var segment_nz_nc = rep1(alt(unreserved, pct_encoded, sub_delims, "@"));

        var path_abempty = rep0("/", segment);
        var path_absolute = seq("/", opt(segment_nz, rep0("/", segment)));
        var path_noscheme = seq(segment_nz_nc, rep0("/", segment));
        var path_rootless = seq(segment_nz, rep0("/", segment));
        var path_empty = seq("");

        var path = alt(path_abempty, path_absolute, path_noscheme, path_rootless, path_empty);

        var query = rep0(alt(pchar,'/','?'));

        var fragment = rep0(alt(pchar,'/','?'));

        var hier_part = alt(
            seq("//", authority, path),
            path_absolute,
            path_rootless,
            path_empty
        );

        var absolute_URI = seq(scheme, ":", hier_part, opt("?", query));

        // RFC 5234 appendix-B

        var CR = 0x0D;
        var LF = 0x0A;
        var CRLF = seq(CR, LF);
        var SP = 0x20;
        var HTAB = 0x09;
        var WSP = alt(SP, HTAB);
        var VCHAR = range(0x21, 0x7E);



        // rfc7230

        var OCTET = range(0x00, 0xFF); // 8 bits
        var OWS = rep0(alt(SP, HTAB));

        var tchar = alt(ch("!*$%&'*+-.^_`|~"), DIGIT, ALPHA);
        var token = rep1(tchar);

        var HTTP_version = seq("HTTP", "/", DIGIT, ".", DIGIT);

        var absolute_path = rep1("/", segment);
        var origin_form = seq(absolute_path, opt("?", query));
        var absolute_form = absolute_URI;
        var authority_form = authority;  // this could be empty! rfc3986 is relaxed on it.
        var asterisk_form = seq("*");
        var request_target = alt(origin_form, absolute_form, authority_form, asterisk_form);

        var method = token;
        var request_line = seq(method, SP, request_target, SP, HTTP_version, CRLF);

        var status_code = times(3, DIGIT);
        var reason_phrase = rep0(alt(HTAB, SP, VCHAR)); // no obs-text
        var status_line = seq(HTTP_version, SP, status_code, SP, reason_phrase, CRLF);

        var start_line = alt(status_line, request_line);

        var field_name = token;
        var field_vchar = VCHAR;
        var field_content = seq(field_vchar, opt(rep1(alt(SP, HTAB)), field_vchar));
        var field_value = rep0(field_content);   // odd. this does not allow "a b c".
        var header_field = seq(field_name, ":", OWS, field_value, OWS);

        var message_body = rep0(OCTET);

        var HTTP_message = seq(
            start_line,
            rep0(header_field, CRLF),
            CRLF,
            opt(message_body)
        );
        return HTTP_message;
    }


}