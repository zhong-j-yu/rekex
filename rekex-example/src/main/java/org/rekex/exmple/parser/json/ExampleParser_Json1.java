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

public interface ExampleParser_Json1
{
    // define Json grammar in algebraic datatypes,
    // producing a statically-typed parse tree

    // whitespaces ----------------------------------------------------

    String wsChars = " \t\n\r";

    // equivalent to @StrWs, with default whitespace chars
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface Word
    {
        String[] value();
        AnnoMacro<Word, StrWs> toStrWs = StrWs.Macro.of(Word::value, wsChars);
    }

    // zero or more whitespaces
    enum OptWs{ @Word("")I }

    // to skip leading ws
    record Input(OptWs leadingWs, JsonValue value){}

    // tokens --------------------------------------------------------

    enum Comma{ @Word(",") COMMA }

    // we could define enums for all separators like "{", ":"
    // but they are used only once in grammar, so we don't bother.
    // maybe it is nicer to define them all here for clarity.

    // composite datatypes --------------------------------------------

    sealed interface JsonValue{} // the `permits` clause is omitted

    record JsonObject(
        @Word("{")Void PL,
        SepBy<Member, Comma> members,
        @Word("}")Void PR
    ) implements JsonValue{}

    record Member(
        JsonString name,
        @Word(":")Void COLON,
        JsonValue value
    ){}

    record JsonArray(
        @Word("[")Void PL,
        SepBy<JsonValue, Comma> values,
        @Word("]")Void PR
    )implements JsonValue{}

    // simple literals ------------------------------------------------

    enum JsonBoolean implements JsonValue {
        @Word("true") TRUE,
        @Word("false") FALSE;
    }

    enum JsonNull implements JsonValue {
        @Word("null") NULL;
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
    // we play with Either<T1,T2>, an adhoc union type T1|T2.

    record JsonString(
        @Ch(QT)Void QL,
        Either<Char1, BsEsc> [] chars,
        @Ch(QT)Void QR,
        OptWs trailingWs
    )implements JsonValue{}

    record Char1(
        @Ch(range={0x20, 0x10FFFF}, except=BS+QT) int c){}

    record BsEsc(
        @Ch(BS)Void B, Either<EscChar, EscUhhhh> esc){}

    record EscChar(
        @Ch(escChars1)char c){}

    record EscUhhhh(
        @Ch("u")Void U, @Regex("[0-9A-Fa-f]{4}")String hhhh){}

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
        @Regex("[0-9]")char @AtLeast(1)[] ds){}

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


    // test --------------------------------------------------------
    public static PegParser<Input> parser()
    {
        return PegParser.of(Input.class);
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.genJava(ExampleParser_Json1.class, Input.class, null);

        ExampleParserUtil.testInputs("Json1", parser(), input->input.value);
    }


    // convert JsonString and JsonNumber to Java String and BigDecimal -------------------------

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
    // this is rather silly.
    // In later examples, we'll use one regex to match the entire number.
    // Alternatively, use ParseInfo to retain the text that matches JsonNumber.
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


}
