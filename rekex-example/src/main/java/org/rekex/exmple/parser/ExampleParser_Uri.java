package org.rekex.exmple.parser;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.Opt;
import org.rekex.helper.datatype.seq.Seq2;
import org.rekex.parser.PegParser;
import org.rekex.regexp.RegExp;
import org.rekex.spec.Regex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static org.rekex.regexp.RegExpApi.*;

public interface ExampleParser_Uri
{
    // URI, according to rfc3986
    // URI can be matched by regex alone. but we want to extract parts of URI;
    // and for some parts, we want to know which alt rule produced it.
    // so we introduce datatypes to represent those interesting parts.
    // For other parts, we don't care about their internal rules; just use regex.

    // all rules in regex -----------------------------------------------

    RegExp ALPHA = alt(range(0x41, 0x5A), range(0x61, 0x7A));
    RegExp DIGIT = range(0x30, 0x39);
    RegExp HEXDIG = alt(DIGIT, range('A','F'), range('a','f'));

    RegExp pct_encoded = seq("%", HEXDIG, HEXDIG);

    RegExp sub_delims = ch("!$&'()*+,;=");
    RegExp unreserved = alt(ALPHA, DIGIT, ch("-._~"));

    RegExp scheme = seq(ALPHA, rep0( alt(ALPHA, DIGIT, '+', '-', '.') ));

    RegExp userinfo = rep0(alt(unreserved, pct_encoded, sub_delims, ":"));
    RegExp reg_name = rep0(alt(unreserved, pct_encoded, sub_delims));
    RegExp host = alt(reg_name); // IP addresses are omitted here
    RegExp port = rep0(DIGIT);
    RegExp authority = seq(opt(userinfo, "@"), host, opt(":", port));

    RegExp pchar = alt(pct_encoded, unreserved, sub_delims, ":", "@");
    RegExp segment = rep0(pchar);
    RegExp segment_nz = rep1(pchar);
    RegExp segment_nz_nc = rep1(alt(unreserved, pct_encoded, sub_delims, "@"));

    RegExp path_abempty = rep0("/", segment);
    RegExp path_absolute = seq("/", opt(segment_nz, rep0("/", segment)));
    RegExp path_noscheme = seq(segment_nz_nc, rep0("/", segment));
    RegExp path_rootless = seq(segment_nz, rep0("/", segment));
    RegExp path_empty = seq("");

    RegExp path = alt(path_abempty, path_absolute, path_noscheme, path_rootless, path_empty);

    RegExp query = rep0(alt(pchar, '/', '?'));

    RegExp fragment = rep0(alt(pchar, '/', '?'));

    RegExp hier_part = alt(
        seq("//", authority, path),
        path_absolute,
        path_rootless,
        path_empty
    );

    RegExp uri = seq(scheme, ":", hier_part, opt("?", query), opt("#", fragment));

    // RegExp to @Regex ----------------------------------------------------------

    // @Rf("field_name") -> @Regex
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface Rf
    {
        String value();
        AnnoMacro<Rf, Regex> toRegex = thiz-> toRegexAnno(thiz.value());
    }

    static Regex toRegexAnno(String fieldName)
    {
        try
        {
            Field field = ExampleParser_Uri.class.getField(fieldName);
            RegExp exp = (RegExp)field.get(null);
            exp = simplify(exp);
            String regex = toRegex(exp);
            return AnnoBuilder.build(Regex.class, regex);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    // parse tree nodes ---------------------------------------------------------------
    // containing syntax parts; no thinning in this example;

    // we'd like to know which clause in alt() matched a part like `path`.
    // for that we introduce a subtype for each clause.
    // (another design: record T_path(clause_id, str), with multiple Ctor methods)

    sealed interface T_hier_part{}

    sealed interface T_path{ String str(); }

    // ordering of subrules/subtypes is important!
    // better use @Permits to explicitly enumerate them in order.
    // here, for showcase, we depend on their ordering in the source file.

    record T_ss_auth_path(
        @Str("//")String SS, T_authority authority, T_path path) implements T_hier_part{}

    record T_path_abempty(
        @Rf("path_abempty") String str) implements T_path{}
    record T_path_absolute(
        @Rf("path_absolute") String str) implements T_path, T_hier_part{}
    record T_path_noscheme(
        @Rf("path_noscheme") String str) implements T_path{}
    record T_path_rootless(
        @Rf("path_rootless") String str) implements T_path, T_hier_part{}
    record T_path_empty(
        @Rf("path_empty") String str) implements T_path, T_hier_part{}


    // this is pretty disgusting...
    record T_authority(
        Opt<Seq2<
            @Rf("userinfo")String,
            @Ch("@")Void>> opt_userinfo,

        @Rf("host")String host,

        Opt<Seq2<
            @Ch(":")Void,
            @Rf("port")String>> opt_port
    )
    {
        public String userinfo(){
            return opt_userinfo.caseOf(Seq2::v1, ()->null);
        }
        public String port(){
            return opt_port.caseOf(Seq2::v2, ()->null);
        }
    }

    // less disgusting...
    record S_query(@Ch("?")Void S, @Rf("query")String query){}

    record S_fragment(@Ch("#")Void S, @Rf("fragment")String fragment){}

    // the root type
    record T_uri(
        @Rf("scheme")String scheme,

        @Ch(":")Void COLON,

        T_hier_part hier_part,

        Opt<S_query> opt_query,

        Opt<S_fragment> opt_fragment
    )
    {
        public String query(){
            return opt_query.caseOf(S_query::query, ()->null);
        }
        public String fragment(){
            return opt_fragment.caseOf(S_fragment::fragment, ()->null);
        }
    }

    public static PegParser<T_uri> parser()
    {
        return PegParser.of(T_uri.class);
    }

    public static void main(String[] args)
    {
        ExampleParserUtil.testInputs("URI", parser(), null);
    }
}
