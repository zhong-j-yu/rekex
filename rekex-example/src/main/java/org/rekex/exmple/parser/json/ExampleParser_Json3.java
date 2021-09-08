package org.rekex.exmple.parser.json;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.annotype.AnnoType;
import org.rekex.annotype.ClassType;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy;
import org.rekex.helper.datatype.alt.Alt2;
import org.rekex.helper.datatype.alt.Alt4;
import org.rekex.helper.datatype.alt.Alt6;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserBuilder;
import org.rekex.spec.Regex;

import java.lang.annotation.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.rekex.exmple.parser.json.ExampleParser_Json1.OptWs;
import org.rekex.exmple.parser.json.ExampleParser_Json1.Token;
import org.rekex.exmple.parser.json.ExampleParser_Json2.RegexNumber;

// In this example, we produce ASTs with ordinary Java types:
//   Map<String,Object>, List<Object>, String, BigDecimal, Boolean, (Void)null.
// The root type is Object
//
// while this is cool, it's probably not a common use case.
// Json mirrors standard & generic datatypes in a host language.
// That's not true in most grammars which call for custom datatypes.
//
public interface ExampleParser_Json3
{
    // tokens --------------------------------------------------------

    enum Comma{ @Token(",") COMMA }

    String QT = "\"";
    String BS = "\\";

    // to strip leading whitespaces
    record Input(OptWs leadingWs, Object value){}

    // rules --------------------------------------------------------

    public class RulesCatalog
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


        public Map<String, Object> object(@Token("{") char PL, SepBy<Member, Comma> members, @Token("}") char PR)
        {
            // the following code doesn't work because the API doesn't allow `null` in value
            //   return members.values().stream().collect(Collectors.toMap(Member::name, Member::value)); // throws on duplicate keys
            HashMap<String,Object> map = new HashMap<>();
            for(var member : members.values())
                map.put(member.name, member.value); // tolerate duplicate keys
            return map;
        }

        public record Member(String name, @Token(":") char COLON, Object value){}

        public List<Object> array(@Token("[") char PL, SepBy<Object, Comma> values, @Token("]") char PR)
        {
            return values.values();
        }

        public Boolean trueV(@Token({"true"}) String str)
        {
            return Boolean.TRUE;
        }

        public Boolean falseV(@Token({"false"}) String str)
        {
            return Boolean.FALSE;
        }

        public Void nullV(@Token("null") String str)
        {
            return null;
        }

        public BigDecimal number(@RegexNumber String str, OptWs trailingWs)
        {
            return new BigDecimal(str);
        }




        // string .............

        // un-annotated `int` to match a logical json character in strings

        public String string(@Ch(QT) char QL, int[] chars, @Ch(QT) char QR, OptWs trailingWs)
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

        // \b etc
        public int escC(@Ch(BS) char BSL, @Ch(escN) char c)
        {
            int i = escN.indexOf(c);
            assert i!=-1;
            return escV.charAt(i);
        }

        // \u1234
        public int escU(@Ch(BS) char BSL, @Ch("u") char U, byte h1, byte h2, byte h3, byte h4)
        {
            return (h1<<12) | (h2<<8) | (h3<<4) | (h4) ;
        }

        // `byte` as a hex char
        public byte hex(@Regex("[0-9A-Fa-f]")char h)
        {
            // ascii order:  0..9..A..Z..a..z
            if(h<='9') return (byte)(h-'0');
            if(h>='a') return (byte)(h-'a'+10);
            return (byte)(h-'A'+10);
        }
    }

    // test ----------------------------------------------------

    public static PegParser<Object> parser()
    {
        return new PegParserBuilder()
            .rootType(Input.class)
            .catalogClass(RulesCatalog.class)
            //.logGrammar(System.out::println)
            .build(new RulesCatalog());
    }

    public static void main(String[] args)
    {
        ExampleParserUtil.testInputs("Json3", parser(), obj->obj);
    }

}
