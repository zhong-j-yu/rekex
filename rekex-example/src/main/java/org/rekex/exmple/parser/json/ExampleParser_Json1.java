package org.rekex.exmple.parser.json;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.AtLeast;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.StrWs;
import org.rekex.helper.datatype.Either;
import org.rekex.helper.datatype.Opt;
import org.rekex.helper.datatype.SepBy;
import org.rekex.parser.PegParser;
import org.rekex.spec.Regex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

// parse tree datatype JsonValue and subtypes
// syntax rules are define by class constructors.
// nodes contain syntax stuff like comma; not thinned.
public interface ExampleParser_Json1
{
    sealed interface JsonValue{}

    // whitespaces ----------------------------------------------------

    String wsChars = " \t\n\r";

    // equivalent to @StrWs, with default whitespace chars
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface Token
    {
        String[] value();
        AnnoMacro<Token, StrWs> toStrWs = StrWs.Macro.of(Token::value, wsChars);
    }

    // zero or more whitespaces
    enum OptWs{ @Token("")I }

    // to skip leading ws
    record Input(OptWs leadingWs, JsonValue value){}

    // tokens --------------------------------------------------------

    enum Comma{ @Token(",") COMMA }
    // we could define enums for all separators like "{", ":"
    // but they are used only once in grammar, so we don't bother.
    // maybe it is nicer to define them all here for clarity.

    // simple literals ------------------------------------------------

    enum JsonBoolean implements JsonValue {
        @Token("true") TRUE,
        @Token("false") FALSE;
    }

    enum JsonNull implements JsonValue {
        @Token("null") NULL;
    }

    // string and number literals -------------------------------------
    //
    // could be treated as atomic tokens, using regex to match the whole thing.
    // but in this example, the parse tree contains their internal structures.

    // json string

    String BS = "\\";
    String QT = "\"";
    String escChars1 = BS+QT+"/bfnrt";
    String escChars2 = BS+QT+"/\b\f\n\r\t";

    // as an example, instead of introducing a supertype for T1,T2,
    // we play with Either<T1,T2>, basically an adhoc union type T1|T2.

    record JsonString(
        @Ch(QT)char QL,
        Either<Char1, BsEsc> [] chars,
        @Ch(QT)char QR,
        OptWs trailingWs
    )implements JsonValue{}

    record Char1(
        @Ch(range={0x20, 0x10FFFF}, except=BS+QT) int c){}

    record BsEsc(
        @Ch(BS)char B, Either<EscChar, EscUhhhh> esc){}

    record EscChar(
        @Ch(escChars1)char c){}

    record EscUhhhh(
        @Ch("u")char U, @Regex("[0-9A-Fa-f]{4}")String hhhh){}

    // json number

    record JsonNumber(
        Opt<@Ch("-")Character> optMinus,
        Either<DigitsNZ, Digit> unsignedInt, // notice the ordering of 2 rules
        Opt<Fraction> optFraction,
        Opt<Exponent> optExponent,
        OptWs trailingWs
    )implements JsonValue{}

    record Digit(
        @Regex("[0-9]")char d){}

    record Digits(
        @Regex("[0-9]")char@AtLeast(1)[] ds){}

    record DigitsNZ( // a non-zero digit, followed by one or more digits
        @Regex("[1-9]")char d1, Digits more){}

    record Fraction(
        @Ch(".")char DOT,
        Digits digits
    ){}
    record Exponent(
        @Ch("Ee")char E,
        Opt<@Ch("+-")Character> optSign,
        Digits digits
    ){}


    // composite datatypes -----------------------------------------------------

    record JsonObject(
        @Token("{")char PL,
        SepBy<Member, Comma> members,
        @Token("}")char PR
    ) implements JsonValue{}

    record Member(
        JsonString name,
        @Token(":") char COLON,
        JsonValue value
    ){}

    record JsonArray(
        @Token("[")char PL,
        SepBy<JsonValue, Comma> values,
        @Token("]")char PR
    )implements JsonValue{}

    // convert to Java String and BigDecimal -------------------------

    static String toJavaString(JsonString jStr)
    {
        StringBuilder sb = new StringBuilder(jStr.chars.length);
        for(var ch1_bse : jStr.chars)
            ch1_bse.caseOf(  // might be nicer to use Alt3 for 3 cases.
                ch1->sb.append(ch1.c),
                bse->bse.esc.caseOf(
                    ec->sb.append(escChars2.charAt(escChars1.indexOf(ec.c))),
                    eh->sb.append((char)Integer.parseInt(eh.hhhh, 0, 4, 16))
                )
            );
        return sb.toString();
    }

    // we'll reconstruct the whole string from parts, hand it to another parser.
    // this is rather silly. Later, we'll use one regex to match the entire number.
    static BigDecimal toBigDecimal(JsonNumber jn)
    {
        StringBuffer sb = new StringBuffer();
        jn.optMinus.ifPresent(ch ->
            sb.append(ch.charValue())
        );
        jn.unsignedInt.caseOf(
            digits->sb.append(digits.d1).append(digits.more.ds),
            digit ->sb.append(digit.d)
        );
        jn.optFraction.ifPresent(frac -> {
            sb.append(frac.DOT);
            sb.append(frac.digits.ds);
        });
        jn.optExponent.ifPresent(expo -> {
            sb.append(expo.E);
            expo.optSign.ifPresent(sign->
                sb.append(sign.charValue())
            );
            sb.append(expo.digits.ds);
        });
        return new BigDecimal(sb.toString());
    }

    // test --------------------------------------------------------
    public static PegParser<Input> parser()
    {
        return PegParser.of(Input.class);
    }

    public static void main(String[] args)
    {
        ExampleParserUtil.testInputs("Json1", parser(), input->input.value);
    }
}
