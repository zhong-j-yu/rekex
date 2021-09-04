package org.rekex.exmple.parser.json;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserBuilder;
import org.rekex.regexp.RegExpApi;
import org.rekex.spec.Regex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rekex.exmple.parser.json.ExampleParser_Json1.OptWs;
import org.rekex.exmple.parser.json.ExampleParser_Json1.Token;

// in this example, we produce ASTs with Gson's JsonElement types.
// it's a 3rd party library we cannot modify.
// a rule catalog is used to match Json syntax and produce JsonElement.
public interface ExampleParser_Json2
{
    // tokens --------------------------------------------------------

    enum Comma{ @Token(",") COMMA }

    // we don't care about the internal structure of a number.
    // create one regex to match the whole thing.
    // trailing whitespaces are not handled here.
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface RegexNumber
    {
        AnnoMacro<RegexNumber, Regex> toRegex = thiz -> new RegExpApi(){
            final Regex result;
            {
                var digit = range('0', '9') ;
                var digits = rep1(digit) ;
                var digitsNZ = seq(range('1', '9'), digits) ;
                var integer = seq(opt('-'), alt(digitsNZ, digit)) ; // notice the ordering
                var fraction = seq('.', digits) ;
                var exponent = seq(ch("Ee"), opt(ch("+-")), digits) ;
                var number = seq(integer, opt(fraction), opt(exponent)) ;
                var regex = toRegex(number);
                result = AnnoBuilder.build(Regex.class, regex);
            }}.result;
    }

    // but we do care about the internal structure of a string,
    // which we'll need to convert it to a java String.

    String BS = "\\";
    String QT = "\"";
    String escChars1 = BS+QT+"/bfnrt";
    String escChars2 = BS+QT+"/\b\f\n\r\t";

    // gson interfaces -----------------------------------------------------
    // we don't want to actually import the whole library just for this example;
    // so we just copy their APIs here, which is legal according to their lawsuit.

    abstract class JsonElement{}

    class JsonObject extends JsonElement{
        public final Map<String,Object> nameValues = new HashMap<>();
        public void add(String name, Object value){
            nameValues.put(name, value);
        }
        public String toString(){ return nameValues.toString(); }
    }
    class JsonArray extends JsonElement{
        public final List<JsonElement> elements = new ArrayList<>();
        public void add(JsonElement element){
            elements.add(element);
        }
        public String toString(){ return elements.toString(); }
    }
    class JsonPrimitive extends JsonElement{
        public final Object object;
        public JsonPrimitive(Object object){
            this.object = object;
        }
        public String toString(){ return String.valueOf(object); }
    }
    class JsonNull extends JsonElement{
        public static final JsonNull INSTANCE = new JsonNull();
        public String toString(){ return "null"; }
    }

    // ignore leading whitespaces in input
    record Input(JsonElement value){}

    // rules --------------------------------------------------------

    class RulesCatalog
    {
        public Input input(OptWs leadingWs, JsonElement value)
        {
            return new Input(value);
        }

        public JsonObject object(@Token("{") char PL, SepBy<Member, Comma> members, @Token("}") char PR)
        {
            JsonObject obj = new JsonObject();
            for(var member : members.values())
                obj.add(member.name, member.value);
            return obj;
        }

        // intermediary data carrier; won't be present in the final AST.
        // the syntactic structure is defined not here, but by member() ctor.
        public record Member(String name, JsonElement value){}

        public Member member(JsonPrimitive name, @Token(":") char COLON, JsonElement value)
            throws IllegalArgumentException // semantic predicate
        {
            if(name.object instanceof String str)
                return new Member(str, value);
            // the name must be a json string; but we only have the JsonPrimitive type.
            // we could use an annotation as the distinguisher.
            // but here, we use semantic predicate instead, to fail on non-string.
            // it is not fatal, and the parser will try alternative paths.
            throw new IllegalArgumentException("object member name must be a string");
            // actually, since there's no alternative path here,
            // we could raise an undeclared exception to stop the parsing immediately.
        }

        public JsonArray array(@Token("[") char PL, SepBy<JsonElement, Comma> values, @Token("]") char PR)
        {
            JsonArray array = new JsonArray();
            for(var value : values.values())
                array.add(value);
            return array;
        }

        public JsonPrimitive trueV(@Token("true") String str)
        {
            return new JsonPrimitive(Boolean.TRUE);
        }

        public JsonPrimitive falseV(@Token("false") String str)
        {
            return new JsonPrimitive(Boolean.FALSE);
        }

        public JsonNull nullV(@Token("null") String str)
        {
            return JsonNull.INSTANCE;
        }

        public JsonPrimitive number(@RegexNumber String str, OptWs trailingWs)
        {
            var num = new BigDecimal(str);
            return new JsonPrimitive(num);
        }


        // a logical character inside a string literal
        public record JsonChar(int c){}

        public JsonPrimitive string(@Ch(QT) char QL, List<JsonChar> chars, @Ch(QT) char QR, OptWs trailingWs)
        {
            StringBuilder sb = new StringBuilder(chars.size());
            for(var jc : chars)
                sb.appendCodePoint(jc.c);
            return new JsonPrimitive(sb.toString());
        }

        public JsonChar char1(@Ch(range={0x20, 0x10FFFF}, except=BS+QT) int c)
        {
            return new JsonChar(c);
        }

        public JsonChar escC(@Ch(BS) char BSL, @Ch(escChars1) char c)
        {
            int i = escChars1.indexOf(c);
            assert i!=-1;
            var c2 = escChars2.charAt(i);
            return new JsonChar(c2);
        }

        public JsonChar escU(@Ch(BS) char BSL, @Ch("u") char U, @Regex("[0-9A-Fa-f]{4}") String hhhh)
        {
            char c = (char)Integer.parseInt(hhhh, 16);
            return new JsonChar(c);
        }
    }

    // test --------------------------------------------------------
    public static PegParser<Input> parser()
    {
        return new PegParserBuilder()
            .rootType(Input.class)
            .catalogClass(RulesCatalog.class)
            .build(new RulesCatalog());
    }

    public static void main(String[] args)
    {
        ExampleParserUtil.testInputs("Json2", parser(), input->input.value);
    }
}
