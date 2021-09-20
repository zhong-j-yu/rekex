package org.rekex.exmple.parser.json;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.StrWs;
import org.rekex.helper.datatype.SepBy;
import org.rekex.helper.datatype.alt.Alt6;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserBuilder;
import org.rekex.spec.Regex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rekex.exmple.parser.json.ExampleParser_Json2.RegexNumber;

public interface ExampleParser_Json3
{
    // In this example, we produce ASTs in ordinary Java types:
    //
    //   Object
    //     Map<String,Object>
    //     List<Object>
    //     String
    //     BigDecimal
    //     Boolean
    //     (Void)null
    //
    // these types are also used to mirror grammar symbols,
    // e.g. List<Object> as Json array; Object as Json value.
    //
    // While this is cool, it's probably not a common use case.
    // Json mirrors standard & generic datatypes in a host language.
    // That's not true in most grammars which call for custom datatypes.
    //


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
    record Input(OptWs leadingWs, Object value){}


    // tokens --------------------------------------------------------

    enum Comma{ @Word(",") COMMA }

    // rules --------------------------------------------------------

    public class CtorCatalog
    {

        public Object anyValue(Alt6<
            Map<String, Object>,
            List<Object>,
            Boolean,
            Void,
            BigDecimal,
            String> alt)
        {
            return alt.value();
        }


        public Map<String, Object> object(@Word("{")Void PL, SepBy<Member, Comma> members, @Word("}")Void PR)
        {
            // return members.values().stream().collect(Collectors.toMap(Member::name, Member::value));
            // that didn't work because of `null` values.

            HashMap<String,Object> map = new HashMap<>();
            for(var member : members.values())
                map.put(member.name, member.value); // duplicate keys are tolerated
            return map;
        }

        // intermediary construct
        public record Member(String name, @Word(":") char COLON, Object value){}

        public List<Object> array(@Word("[")Void PL, SepBy<Object, Comma> values, @Word("]")Void PR)
        {
            return values.values();
        }

        public Boolean trueV(@Word("true") String str)
        {
            return Boolean.TRUE;
        }

        public Boolean falseV(@Word("false") String str)
        {
            return Boolean.FALSE;
        }

        public Void nullV(@Word("null") String str)
        {
            return null;
        }

        // @RegexNumber is imported from ExampleParser_Json2
        public BigDecimal number(@RegexNumber String str, OptWs trailingWs)
        {
            return new BigDecimal(str);
        }




        // string ........................

        static final String QT = "\"";
        static final String BS = "\\";

        // un-annotated primitives as datatypes that mirror grammar symbols
        //    `int` represents a logical json character in strings.
        //   `byte` represents a hex char.
        // this can be quite confusing to casual observers.

        public String string(@Ch(QT)Void QL, int[] chars, @Ch(QT)Void QR, OptWs trailingWs)
        {
            return new String(chars, 0, chars.length);
        }

        // unescape char
        public int char1(@Ch(range={0x20, 0x10FFFF}, except=BS+QT) int c)
        {
            return c;
        }

        final static String escN = BS+QT+"/bfnrt";
        final static String escV = BS+QT+"/\b\f\n\r\t";

        // escaped char: \b etc.
        public int escC(@Ch(BS)Void BSL, @Ch(escN) char c)
        {
            int i = escN.indexOf(c);
            assert i!=-1;
            return escV.charAt(i);
        }

        // escaped char: \u1234
        public int escU(@Ch(BS)Void BSL, @Ch("u")Void U, byte h1, byte h2, byte h3, byte h4)
        {
            return (h1<<12) | (h2<<8) | (h3<<4) | (h4) ;
        }

        public byte hex(@Regex("[0-9A-Fa-f]")char h)
        {
            // ascii order:  0..9..A..Z..a..z
            if(h<='9') return (byte)(h-'0');
            if(h>='a') return (byte)(h-'a'+10);
            return (byte)(h-'A'+10); // 0 - 0xF
        }
    }

    // test ----------------------------------------------------

    public static PegParser<Object> parser()
    {
        return new PegParserBuilder()
            .rootType(Input.class)
            .catalogClass(CtorCatalog.class)
            //.logGrammar(System.out::println)
            .build(new CtorCatalog());
    }

    public static void main(String[] args)
    {
        ExampleParserUtil.testInputs("Json3", parser(), obj->obj);
    }

}
