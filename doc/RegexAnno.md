
## @Regex and Equivalent Annotations

The [@Regex](../rekex-grammar/src/main/java/org/rekex/spec/Regex.java) annotation is used by Rekex 
to match inputs with regular expressions.
It has the following attributes:

- `Regex.value` - the regular expression string, 
  as defined by [java.util.regex.Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html)
- `Regex.flags` -deafult to 0; 
  see [Pattern.flags()](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html#flags())
- `Regex.group` - the group number of the capturing group to be extracted, default to 0;
   see [Matcher.group(group)](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Matcher.html#group(int)) 

In the example of `@Regex(value="(true)\\s*", group=1) String`,
the regular expression matches and consumes any input with literal `"true"`
followed by zero or more whitespaces; however the `String` will only contain `"true"`.

Note that Java regular expression works at the unit of "code point", not Java's `char`.

Other annotations can be used in place of `@Regex` if they are convertible to `@Regex`
through [AnnoMacro](./AnnoMacro.md).
For example, you can define `@Hex` as equivalent to `@Regex("[0-9A-Fa-f]")`

        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        @interface Hex
        {
            AnnoMacro<Hex,Regex> toRegex = thiz->
                AnnoBuilder.build(Regex.class, "[0-9A-Fa-f]");
        }

>The annotation must be prefixed with
`@Target(ElementType.TYPE_USE) @Retention(RetentionPolicy.RUNTIME)`.

Rekex ships with a few commonly used annotation macros:

[@Ch](../rekex-grammar/src/main/java/org/rekex/helper/anno/Ch.java) 
-- to match a single code point,
if it's contained in a set defined by union of `Ch.value` and `Ch.range`, minus `Ch.except`.

- `Ch.value` - a String with zero or more code points, e.g. `@Ch("a\n{")`
- `Ch.range` - zero or more ranges of code points, e.g. `@Ch(range={'0','9','A','F'})`
- `Ch.except` - a String with zero or more code points, e.g. `@Ch(range={' ',0x10FFFF}, except="\"\\")`
- `Ch.ignoreCase` - case-insensitive matching if true.

[@Str](../rekex-grammar/src/main/java/org/rekex/helper/anno/Str.java)
-- to match a string literal,
if it's contained in a set defined by `Str.value`

- `Str.value` - a set of String literals, e.g.
    - `@Str({"abc", "xyz})` - matches `"abc"` or `"xyz"`
    - `@Str("abc")` - matches `"abc"`
    - `@Str("")` - matches `""` a.k.a `ε`
    - `@Str({})` - never matches any input
- `Str.ignoreCase` - case-insensitive matching if true.

[@StrWs](../rekex-grammar/src/main/java/org/rekex/helper/anno/StrWs.java)
-- similar to `@Str`, but also skips 
zero or more trailing whitespaces defined by `StrWs.wsChars`.
For example, `@StrWs(value="true", wsChars=" \t") String`
matches `"true"` followed by zero or more space/tab chars; 
the `String` value will only contain `"true"`.

- `StrWs.wsChars` - a String with zero or more code points,
    as the set of whitespace characters.
  

Most likely, you'll need to define your annotation that's
convertible to `@StrWs` with a default `wsChars`

        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        @interface Word
        {
            String[] value();
            AnnoMacro<Word, StrWs> toStrWs = StrWs.Macro.of(Word::value, " \t\r\n");
        }

## create @Regex equivalent with RegExp

[RegExp](./RegExp.md) API can be used to compose complex regular expressions.

        class RegexUtil extends RegExpApi // inheriting methods from RegExpApi
        {
            static String signedInt()
            {
                var digit = range('0', '9');
                var digits = rep1(digit);
                var nzDigits = seq(range('1', '9'), digits);
                var unsigned = alt(nzDigits, digit); 
                var signed = seq(opt('-'), unsigned);
                return toRegex(signed);
            }
        }

        @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
        @interface Int
        {
            AnnoMacro<Int,Regex> toRegex = thiz->
                AnnoBuilder.build(Regex.class, RegexUtil.signedInt());
        }

----
*Create by [Zhong Yu](http://zhong-j-yu.github.io)*

