
# RegExp - structured regular expression

Regular expression (regex) got a bad reputation because of its typical compact
representation which reads like an alphabet soup, obfuscating
the structure within. It is also quite difficult to compose a complex regex by hand.

The RegExp library solves this problem by providing factory methods like `seq(...), alt(...)`
to construct a regular expression as a tree.
The construction code will be long and wordy, by design,
so that the structure is understandable at first glance.

For example, we want a regex to match time format `HH:MM:SS`, optionally 
with `.mmm` for milliseconds.

        var digit = range('0', '9');   // 0-9
        var HH = alt(
            seq(alt('0','1'), digit),  // 00-19
            seq('2', ch("0123")),      // 20-23
            "24"                       // 24, sake of example
        );
        var MM = seq(range('0', '5'), digit); // 00-59
        var SS = MM;
        var mmm = times(3, digit);    // 000-999

        var time = seq(HH, ":", MM, ":", SS, opt(".", mmm));

This will create a tree of subexpressions, but we don't need to care about that.
All we want is to obtain the regex in its typical string representation

        time = simplify(time);

        String regex = toRegex(time);
        
        "(?:[01][0-9]|2[0123]|24):[0-5][0-9]:[0-5][0-9](?:\\.[0-9]{3})?"

All factory methods are static methods from 
[RegExpApi](../rekex-regexp/src/main/java/org/rekex/regexp/RegExpApi.java); 
they can be accessed by 

    import static rekex.regexp.RegExpApi.*;

RegExp fully conforms to 
[java.util.regex.Pattern](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/regex/Pattern.html),
supporting all of its features.

RegExp is part of [Rekex](../README.md), 
but can be used standalone as

        <dependency>
            <groupId>org.rekex</groupId>
            <artifactId>rekex-regexp</artifactId>
            <version>1.0.0</version>
        </dependency>



## character

In this document, "character" refers to unicode characters, ranging from U+0000 to U+10FFFF.
Lone surrogates, i.e. code points in 0xD800 - 0xDFFF, should not appear alone,
either in regex or in input, as the behavior isn't well defined.
`java.lang.String` will be interpreted as an `int[]` of characters, not as `char[]`.

The factory methods accept `char` and `int` arguments representing single characters.

        var CR = '\r';   // char
        var LF = 0x0A;   // int

        var CRLF = seq(CR, LF);


## seq(...)

`seq(r1, r2, ...)` concatenates subexpressions

        var CRLF = seq(CR, LF);

A `String` is interpreted as a `seq` of characters
by almost all factory methods (except in `ch(String)`).

        var TRUE = seq("true"); // equivalent to seq('t', 'r', 'u', 'e')

        var taut = seq(TRUE, '=', "true");  // "true=true"

`seq()` with no arg matches the empty string, a.k.a. `ε` 

        var epsilon = seq(); // or seq("")



## alt(...)

`alt(r1, r2, ...)` means `r1 or r2 or ...`

        var bool = alt(
            TRUE,
            "false"
        );

        var abc = alt(
            'a',
            'b',
            'c'
        );

`ch(String)` is interpreted as `alt` of characters in the string

        var abc_ = ch("abc"); // alt('a','b','c')

`range(x, y)` has the effect of `alt(x, x+1, ..., y)`

        var digit = range('0', '9');

Use `except(...)` to exclude some characters; explained in detail later in "Character Class"

        var ab_1_9 = alt(abc, digit).except('c', '0');

Sometimes `ε` is used as the last clause of an `alt`

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

`alt(r1)` is just `r1` itself; it can be used stylishly,
for example, to uniformly present all grammar terms in alt form.

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

        var opt_sign = opt(sign);  // optional
        var digits0 = rep0(digit); // 0 or more digits
        var digits1 = rep1(digit); // 1 or more digits

If there are more than one arguments to these methods,
they are interpreted as a sequence, e.g.
`opt(r1, r2)` means `opt(seq(r1, r2))`

        var t2 = seq(digit, opt(sign, digit)); 

To quantify repetitions with min/max

        var d4     = times(4, digit);     // 4 digits
        var d4_8   = times(4, 8, digit);  // 4 to 8 digits
        var d4_INF = times(4, Long.MAX_VALUE, digit); // 4 or more

To change *greediness* to *reluctant* or *possessive*

        var rd = reluctant(digits0);
        var pd = possessive(digits0);


## group and reference

A part of a regex can be grouped (by capturing group),
so that it can be referenced later, by 
[Matcher](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/regex/Matcher.html), 
or by back-reference.

- **named group** can be referenced by its name

        var g1 = group("g1", HH);
        var ref1  = backRef("g1");
        var ref1_ = backRef(g1);

- **unnamed group** can be referenced by its group number

        var g2 = group(MM);
        var ref2 = backRef(2); 

The group number of a group (unnamed or named) within a regex can be found by

        var exp2 = seq(g1, ":", g2, ",", ref2);
        int g2_num = findGroupNumberIn(g2, exp2); // 2



Note that `ref2` cannot depend on `g2_num`, 
because that would've caused a circular dependency of
`ref2 -> g2_num -> exp2 -> ref2`.
You may count the group number yourself for `ref2`,
and write a unittest to assert that it's the same as `g2_num`. 

Or you should just prefer named groups and named references
which are much simpler and clearer. 


## atomic group 

An atomic group in regex prevents backtracking.
In the javadoc, it's referred to as `an independent, non-capturing group`

        var digits_all = atomicGroup(rep0(digit));



## lookahead, lookbehind, boundary

`lookahead(boolean, exp)`, `lookbehind(boolean, exp)`
constructs a positive/negative lookahead/lookbehind of `exp`.

        var notLedByZero = lookahead(false, seq("0", digit));
        var number = seq(notLedByZero, rep1(digit));
        // does not match if "0" is followed by more digits

Use `boundary_xxx()` methods for predefined boundary matchers 

        var EOF = boundary_end_of_input();



## flags
To turn on/off 
[flags](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/regex/Pattern.html#flags()) 
on a regex

        var bool_ci = flag(true, Pattern.CASE_INSENSITIVE, bool);

There's a convenience method `ignoreCase(exp)` to turn on `CASE_INSENSITIVE`

        var bool_ci2 = ignoreCase(bool);




## to regex

After you've constructed the root expression,
it's time to convert it to the usual regex format.

        String regex = toRegex(exp);

You can visually inspect the tree structure by

        System.out.println( toTreeText(exp) );

You can try `simplify` which may produce a shorter and nicer regex;
but don't expect it to be too smart.

        exp = simplify(exp);





## opaque regex

You can quote a regex as is, if we don't have a factory method for it

        var unicode_linebreak = opaque("\\R");

For boundaries,

        var unicode_cluster_boundary = boundary("\\b{g}");

For predefined character classes (see later)

        var wordChar = predefined("\\w");
        var UPPER = predefined("\\p{Upper}");
        var beta = predefined("\\N{GREEK SMALL LETTER BETA}");



## Character Class

So far, we have not talked about "character classes".
It is an important construct for implementations of regex, 
but it's not an essential concept of regular expression.
If you use RegExp, you'll rarely need to think about character classes.
But here it goes.

A character class is nothing but a set of characters.
As a regex, a character class matches a single character if it's in the set.
Therefore, it is equivalent to an `alt` of characters.
More complex character classes can be constructed by set union/intersection/negation.

Character classes include:

- A single character is a character class of itself.

- A predefined character class is a predefined set of characters.
  >Avoid using predefined character classes;
  >they are shorthand notations to ease writing regex by hand. 
  >Instead, explicitly and clearly define the set of characters in your code.

- Union of character classes

        var xyz = union('x', 'y', 'z');
                                     
  `union(args)` is equivalent to `alt(args)`  

        var xyz2 = alt('x', 'y', 'z');

- Intersection of character classes

        var intersect = intersect(ch("abc"), ch("bcd")); // alt('b','c')

  `intersect()` with no arg means the set of all characters

        var all = intersect(); // range(0, 0x10FFFF);

- Negation of a character class

        var nonDigit = negate(digit); // any character except 0-9

        var ASCII = range(0x20, 0x7E);
        var nonDigitAscii = intersect(ASCII, negate(digit));

As you see, `union` can always be represented by `alt` instead; 
`intersect` and `negate` are most often used together for set subtraction,
which can be done by `cc1.except(cc2)` instead

        var nonDigit2 = all.except(digit);

        var nonDigitAscii2 = ASCII.except(digit);


## Rekex

There is no way (in Java's dialect) to construct a regex
which references itself in a subexpression. 
If a grammar is recursive, we need to go beyond regular expressions.
The parent project, [Rekex](../README.md), 
is a PEG parser generator that derives grammar rules 
from datatypes of the parse tree. 




----
<sub>Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.</sub>
