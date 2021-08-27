package org.rekex.exmple.parser.json;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.annotype.AnnoType;
import org.rekex.annotype.ClassType;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy;
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

// In this example, we produce ASTs with ordinary Java Object types:
//   Map<String,Object>, List<Object>, String, BigDecimal, Boolean, null.
// Instead of introducing wrapper types, we use annotated types
// `@JV Object` (and subtypes like @JV Boolean) as datatypes of rules.
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

    // rules --------------------------------------------------------

    public class RulesCatalog
    {
        // to strip leading whitespaces, it's simpler to introduce
        //    a wrapper type Input(Object) as the root type.
        // for the sake of this example, we use an annotation instead;
        // the root type is `@Input()Object`, not too friendly to use.

        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        public @interface Input{}

        public @Input()Object input(OptWs leadingWs, @JV Object value)
        {
            return value;
        }





        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        public @interface JV{}

        public @JV Map<String, Object> object(@Token("{") char PL, SepBy<Member, Comma> members, @Token("}") char PR)
        {
            // the following code doesn't work because the API doesn't allow `null` in value
            //   return members.values().stream().collect(Collectors.toMap(Member::name, Member::value)); // throws on duplicate keys
            HashMap<String,Object> map = new HashMap<>();
            for(var member : members.values())
                map.put(member.name, member.value); // tolerate duplicate keys
            return map;
        }

        public record Member(String name, Object value){}

        public Member member(@JV String name, @Token(":") char COLON, @JV Object value)
        {
            return new Member(name, value);
        }

        public @JV List<Object> array(@Token("[") char PL, SepBy<@JV Object, Comma> values, @Token("]") char PR)
        {
            return values.values();
        }

        public @JV Boolean trueV(@Token({"true"}) String str)
        {
            return Boolean.TRUE;
        }

        public @JV Boolean falseV(@Token({"false"}) String str)
        {
            return Boolean.FALSE;
        }

        public @JV Object nullV(@Token("null") String str)
        {
            return null;
        }

        public @JV BigDecimal number(@RegexNumber String str, OptWs trailingWs)
        {
            return new BigDecimal(str);
        }




        // string .............

        // `@JC int` to match a logical json character in strings
        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        public @interface JC{}

        public @JV String string(@Ch(QT) char QL, @JC int[] chars, @Ch(QT) char QR, OptWs trailingWs)
        {
            return new String(chars, 0, chars.length);
        }

        public @JC int char1(@Ch(range={0x20, 0x10FFFF}, except=BS+QT) int c)
        {
            return c;
        }

        final static String escN = BS+QT+"/bfnrt";
        final static String escV = BS+QT+"/\b\f\n\r\t";

        public @JC int escC(@Ch(BS) char BSL, @Ch(escN) char c)
        {
            int i = escN.indexOf(c);
            assert i!=-1;
            return escV.charAt(i);
        }

        public @JC int escU(@Ch(BS) char BSL, @Ch("u") char U, @Hex char h1, @Hex char h2, @Hex char h3, @Hex char h4)
        {
            return ( h2i(h1,12) | h2i(h2,8) | h2i(h3,4) | h2i(h4,0) );
        }

        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        public @interface Hex{
            AnnoMacro<Hex,Regex> toRegex = thiz->
                AnnoBuilder.build(Regex.class, "[0-9A-Fa-f]");
        }
        static int h2i(char h, int shift)
        {
            // ascii order:  0..9..A..Z..a..z
            if(h<='9') return (h-'0')<<shift;
            if(h>='a') return (h-'a'+10)<<shift;
            return (h-'A'+10)<<shift;
        }

    }

    // test ----------------------------------------------------

    public static PegParser<Object> parser()
    {
        // @Input()Object
        Annotation n_expr = AnnoBuilder.of(RulesCatalog.Input.class).build();
        AnnoType type = new ClassType(Set.of(n_expr), Object.class, List.of());

        return new PegParserBuilder()
            .rootType(type)
            .catalogClass(RulesCatalog.class)
            .build(new RulesCatalog());
    }

    public static void main(String[] args)
    {
        ExampleParserUtil.testInputs("Json3", parser(), obj->obj);
    }

}
