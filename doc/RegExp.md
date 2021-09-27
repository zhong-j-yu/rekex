
# RegExp - structured regular expression

Regular expression (regex) has got a bad reputation because of its typical compact
representation, which is an alphabet soup obfuscating the internal structure. 
It is also quite difficult to compose a complex regex by hand.

The [RegExp library](../rekex-regexp) solves this problem by providing 
factory methods like `seq(e1, e2), alt(e1, e2)` etc.
to create and transform regular expressions as data structures.
The construction code will be long and wordy, by design,
so that the structures can be understood at first glance.

For example, we want a regular expression to match time format `HH:MM:SS`, optionally 
with `.mmm` for milliseconds.

        var digit = range('0', '9');   // 0-9
        var HH = alt(
            seq(alt('0','1'), digit),  // 00-19
            seq('2', ch("0123")),      // 20-23
            "24"                       // 24, for the sake of example
        );
        var MM = seq(range('0', '5'), digit); // 00-59
        var SS = MM;
        var mmm = times(3, digit);    // 000-999

        var time = seq(HH, ":", MM, ":", SS, opt(".", mmm));

Usually we just want to get the string representation of the regex
to interoperate with other software components

        time = simplify(time);

        String regex = toRegex(time);
        
        "(?:[01][0-9]|2[0123]|24):[0-5][0-9]:[0-5][0-9](?:\\.[0-9]{3})?"

See more examples [here](../rekex-example/src/main/java/org/rekex/exmple/regexp)
(including the infamous regex for email address).

RegExp fully conforms to 
[java.util.regex.Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html),
supporting all of its features.

RegExp is part of [Rekex](../README.md), 
but can be used standalone as

        <dependency>
            <groupId>org.rekex</groupId>
            <artifactId>rekex-regexp</artifactId>
            <version>1.1.1</version>
        </dependency>

All factory methods are static methods from
[RegExpApi](../rekex-regexp/src/main/java/org/rekex/regexp/RegExpApi.java),
which can be accessed by

    import static org.rekex.regexp.RegExpApi.*;


## argument types

The datatypes we use to represent regular expressions are 
[RegExp](../rekex-regexp/src/main/java/org/rekex/regexp/RegExp.java) and subtypes.
Most factory methods expect arguments of `RexExp`. For convenience, we also allow
arguments of characters and strings, which are automatically converted to `RegExp`

- `char`, `int` - as a single character
- `String` - as a sequence of characters

We use `Object` as the static types of the arguments to cover all of these types. 

## character

In this document, "character" refers to unicode characters, ranging from U+0000 to U+10FFFF.

        var CR = '\r';   // char
        var LF = 0x0A;   // int

`String` will be interpreted as an `int[]` of characters, not as a `char[]`.

Lone surrogates, i.e. code points in D800-DFFF, should not appear alone,
either in the regular expression or in the input, 
since the behavior [is not very clear](./note-regex-unicode.txt).


## seq(...)

`seq(e1, e2, ...)` concatenates subexpressions

        var CRLF = seq(CR, LF);

`String` arguments are interpreted as `seq` of characters

        var TRUE = seq("true"); // equivalent to seq('t', 'r', 'u', 'e')

        var taut = seq(TRUE, '=', "true");  // "true=true"

`seq()` with no arg matches the empty string, a.k.a. `ε` 

        var epsilon = seq(); // or seq("")



## alt(...)

`alt(e1, e2, ...)` means `e1 or e2 or ...`

        var bool = alt(
            TRUE,
            "false"
        );

        var abc = alt(
            'a',
            'b',
            'c'
        );

`ch(String)` is equivalent to `alt` of characters in the string

        var abc_ = ch("abc"); // alt('a','b','c')

`range(x, y)` is equivalent to `alt(x, x+1, ..., y)`

        var digit = range('0', '9');

Use `except` to exclude some characters from a set of characters, 

        var ab_1_9 = alt(abc, digit).except('c', '0');

> `except` only works with Character Classes, which is explained later.

It's common to see `ε` as the last clause of an `alt`

        var opt_bool = alt(
            "true",
            "false",
            ""
        );

`alt()` with no arg does not match anything.

        boolean enable24 = false;
        var HH = alt(
            seq(ch("01"), digit),
            seq('2', ch("0123")),
            (enable24 ? "24" : alt())  // disabled
        );

`alt(e1)` is just `e1` itself.
It may be used stylishly to present all terms in a uniform fashion

        var PLUS = alt(
            '+'
        );
        var MINUS = alt(
            '-'
        );
        var sign = alt(
            PLUS,
            MINUS
        );


## quantifiers

For `e?`, `e*`, `e+`

        var opt_sign = opt(sign);  // optional

        var digits0 = rep0(digit); // 0 or more digits

        var digits1 = rep1(digit); // 1 or more digits

If there are more than one arguments to these methods,
they are interpreted as a sequence, e.g.
`opt(e1, e2)` is equivalent to `opt(seq(e1, e2))`

        var t2 = seq(digit, opt(sign, digit)); // matches "1", "1+2", etc. 

To quantify repetitions with `min` and `max`

        var d4     = times(4, digit);     // 4 digits
        var d4_8   = times(4, 8, digit);  // 4 to 8 digits
        var d4_INF = times(4, Long.MAX_VALUE, digit); // 4 to infinity

To be *reluctant* or *possessive*

        var rd = reluctant(digits0);
        var pd = possessive(digits0);


## capturing group, back-reference

A subexpression can be enclosed in a *capturing group*,
so that it can be referenced later, by 
[Matcher](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Matcher.html), 
or by back-references.

- **unnamed group** can be referenced by its group number

        var g2 = group(MM);
        var ref2 = backRef(2); 

- **named group** can be referenced by its name (and its group number)

        var g1 = group("g1", HH);
        var ref1  = backRef("g1");
        var ref1_ = backRef(g1);

The group number of a group (unnamed or named) within a regular expression can be found by

        var exp2 = seq(g1, ":", g2, ",", ref2);
        int g2_num = findGroupNumberIn(g2, exp2); // 2



Note that `ref2` cannot depend on `g2_num`, 
because that would've caused a circular dependency of
`ref2 -> g2_num -> exp2 -> ref2`.
You may count the group number for `ref2` by eyeballing,
then write a unittest to assert that it's the same as `g2_num`. 
Or, if you can, stick to named groups and named references. 


## atomic group 

A regex atomic group prevents backtracking.
In the javadoc of
[Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html),
it's referred to as `an independent, non-capturing group`

        var digits_all = atomicGroup(rep0(digit));



## lookahead, lookbehind, boundary

`lookahead(boolean, exp)`, `lookbehind(boolean, exp)`
constructs a positive/negative lookahead/lookbehind

        var notLedByZero = lookahead(false, seq("0", digit)); // negative lookahead

        var number = seq(notLedByZero, rep1(digit));
        // does not match "01"

Use `boundary_xxx()` methods for predefined boundary matchers 

        var EOF = boundary_end_of_input();



## flags
To turn on/off the 
[flags](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html#flags()), 
use `flag(boolean, flags, exp)`

        var abc_ci = flag(true, Pattern.CASE_INSENSITIVE, abc);
 
For the flag 
[CASE_INSENSITIVE](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html#CASE_INSENSITIVE) 
there is a convenience method `ignoreCase(exp)`

        var abc_ci2 = ignoreCase(abc);




## toRegex

After we have constructed the root expression,
it's time to convert it to the typical string representation.

        String regex = toRegex(exp);

We could try `simplify` first which may produce a shorter and nicer regex;
but don't expect it to be too smart.

        exp = simplify(exp);

For diagnosis, the structure of the tree can be displayed visually by

        System.out.println( toTreeText(exp) );





## opaque regex

You can quote a regex as is, if we don't have a factory method for it

        var unicode_linebreak = opaque("\\R");
   
If you know the specific kind of the regex, use the more specific
methods `boundary` and `predefined`

- for boundaries,

        var unicode_cluster_boundary = boundary("\\b{g}");

- for predefined character classes

        var wordChar = predefined("\\w");
        var UPPER = predefined("\\p{Upper}");
        var beta = predefined("\\N{GREEK SMALL LETTER BETA}");

  > A predefined character classe is a shorthand notation for a set of characters, 
  > making it a little easier to write a regex by hand.
  > But with RegExp, you probably should avoid them; instead, defined
  > the set of characters explicitly, e.g. `var UPPER = range('A', 'Z');`



## Character Class

You do not need to worry about *character classes*,
except if you are using the `except()` method.

> Character class is *not* an essential concept of regular expression,
> even though it *is* an important, basic building block
> for *implementations* of regex.

A character class is a regular expression that corresponds to a *set* of characters;
it matches a single input character if and only if 
the set contains the character.
Set union/intersection/negation operations are supported on character classes.

The method `s0.except(s1,s2,...)` is equivalent to 
`intersect(s0, negate(union(s1,s2...)))`,
where `s0, s1, s2, ...` are required to be 
*convertible to character classes* 

The following expressions are convertible to character classes
(assuming `s0, s1, s2, ...` are as well)

- a single character, which is a character class of itself.

- a predefined character class, corresponding to a predefined set of characters.
  
- `seq(s1)`, which is equivalent to `s1`

- `alt(s1,s2,...)`, which is equivalent to `union(s1,s2,...)`

- `union(s1,s2,...)`

        var xyz = union('x', 'y', 'z');
                                     
  `union(args)` can always be expressed as `alt(args)` instead; 
  we recommend the `alt` form over the `union` form. 

        var xyz2 = alt('x', 'y', 'z');

- `intersect(s1,s2,...)`

        var intersect = intersect(ch("abc"), ch("bcd")); // alt('b','c')

  `intersect()` with no arg represents the set of *all* characters

        var all = intersect(); // range(0, 0x10FFFF);

- `negate(s1)`

        var nonDigit = negate(digit); // any character except 0-9

        var ASCII = range(0x20, 0x7E);
        var nonDigitAscii = intersect(ASCII, negate(digit));
                                                                   
- `s0.except(s1,s2,...)`, which is equivalent to
  `intersect(s0, negate(union(s1,s2...)))`

        var nonDigit2 = all.except(digit);

        var nonDigitAscii2 = ASCII.except(digit);


## beyond regex

There is no way (in Java's dialect) to construct a regex
which references itself in a subexpression. 
If a grammar is recursive, we need to go beyond regular expressions.
And even if regex suffices for matching inputs, 
it is not great for extracting data and structures from inputs. 

The parent project, [Rekex](../README.md), 
is a PEG parser generator that derives grammar rules 
from datatypes of the parse tree. 


## References

- [Examples](../rekex-example/src/main/java/org/rekex/exmple/regexp)

- [RegExpApi.java](../rekex-regexp/src/main/java/org/rekex/regexp/RegExpApi.java)

- [RegExp.java](../rekex-regexp/src/main/java/org/rekex/regexp/RegExp.java)

- [java.util.regex.Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html)

## P.S.

Currently this library depends on Java 17 because of the parent project. 
There should be no problem with porting it to a much older Java version,
as a standalone library. (If someone has the time for it, it'd be great.)

It also seems worthwhile to port the API to other programming languages.
The algorithm to serialize the tree to regex is in 
[ToRegex.java](../rekex-regexp/src/main/java/org/rekex/regexp/ToRegex.java).
Please use 
[Issue Tracker](https://github.com/zhong-j-yu/rekex/issues)
for questions and discussions.

----
*Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.*
