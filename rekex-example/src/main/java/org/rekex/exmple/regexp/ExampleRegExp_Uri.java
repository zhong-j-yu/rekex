package org.rekex.exmple.regexp;

import org.rekex.regexp.RegExp;

import static org.rekex.regexp.RegExpApi.*;

// rfc3986; not supported: IP address as host
public class ExampleRegExp_Uri
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

        //var gen_delims = ch(":/?#[]@");
        var sub_delims = ch("!$&'()*+,;=");
        //var reserved = alt(gen_delims, sub_delims);
        var unreserved = alt(ALPHA, DIGIT, ch("-._~"));

        var scheme = seq(ALPHA, rep0( alt(ALPHA, DIGIT, '+', '-', '.') ));

        var userinfo = rep0(alt(unreserved, pct_encoded, sub_delims, ":"));
        var reg_name = rep0(alt(unreserved, pct_encoded, sub_delims));
        var host = alt(reg_name); // IP addresses are omitted here
        var port = rep0(DIGIT);
        var authority = seq(opt(userinfo, "@"), host, opt(":", port));

        var pchar = alt(pct_encoded, unreserved, sub_delims, ":", "@");
        var segment = rep0(pchar);
        var segment_nz = rep1(pchar);
        var segment_nz_nc = rep1(alt(unreserved, pct_encoded, sub_delims, "@"));

        var path_abempty = rep0("/", segment);
        var path_absolute = seq("/", opt(segment_nz, rep0("/", segment)));
        var path_noscheme = seq(segment_nz_nc, rep0("/", segment));
        var path_rootless = seq(segment_nz, rep0("/", segment));
        var path_empty = seq("");

        var path = alt(path_abempty, path_absolute, path_noscheme, path_rootless, path_empty);

        var query = rep0(alt(pchar, '/', '?'));

        var fragment = rep0(alt(pchar, '/', '?'));

        var hier_part = alt(
            seq("//", authority, path),
            path_absolute,
            path_rootless,
            path_empty
        );

        var URI = seq(scheme, ":", hier_part, opt("?", query), opt("#", fragment));

        return URI;
    }

    public static void main(String[] args)
    {
        ExampleRegExpUtil.testInputs(exp(), "URI rfc3986");
    }
}