
# Rekex User's Guide

## Introduction

Rekex is a parser generator that derives grammar rules
from datatypes of the target parse tree.

The current version of Rekex depends on [Java 17](http://jdk.java.net/17/) 
(for `sealed` and `record` types);
supports Parsing Expression Grammar (PEG); 
and provides a recursive decent parser implementation. 

PEG grammar rules are expressed in Java types in the following ways
- alternation and concatenation rules as type hierarchies and constructor/method signatures
- repetition rules as `List<E>` or `E[]`
- positive/negative lookaheads as `Peek<E>` and `Not<E>`
- terminal matching as `@Regex` annotated types

A minimum example that matches `true` or `false`

        enum Bool
        {
            @Str("true") TRUE,
            @Str("false") FALSE,
        }

        PegParser<Bool> parser = PegParser.of(Bool.class);
        Bool bool = parser.matchFull("true");


Given the root datatype of the parse tree, Rekex derives grammar rules recursively, 
and generates a parser for the rules;
the parser in action invokes constructors/methods of datatypes to build the parse tree.

Maven dependency:

        <dependency>
            <groupId>org.rekex</groupId>
            <artifactId>rekex-parser</artifactId>
            <version>1.1.0</version>
        </dependency>

## Alteration Rule as Subtyping

As an example let's build a Json parser, according to the grammar on [json.org](https://www.json.org) .
The root type will be `JsonValue`, which has subtypes `JsonObject`, `JsonArray`, etc.

    sealed interface JsonValue
        permits JsonObject, JsonArray, JsonPrimitive{}

    sealed interface JsonPrimitive extends JsonValue 
        permits JsonString, JsonNumber, JsonBool, JsonNull{}

For the sake of example, we introduced an intermediary type 
`JsonPrimitive` not present in the source grammar.

To Rekex, the parent type `JsonValue` corresponds to an alternation rule, with variant subrules
each corresponding to a subtype; each subtype will correspond to a grammar rule 
that is to be derived, and so forth. 

The parent type must be a `sealed` interface or class so that all subtypes are known to Rekex.

### Order of subtypes

Order of subrules matters; in PEG, the subrules will be attempted in the order they are declared in.

Unfortunately, Java does not define an order on the subtypes in a `permits` clause.
Rekex will try to establish an implicit order based on the declaration order of subtypes 
in the source file; this works if they are declared in the same `.java` file,
and the file is compiled with debug information enabled 
(which is the default behavior of `javac`).

We recommend that you declare all relevant datatypes in one `.java` file;
the declaration order of these types should be consistent with the order required by the grammar.
Preferably they are nested in a top level `public interface`, so that they are implicitly `public` as well;
this document assumes that datatypes are implicitly `public`.

> Note that the `permits` clause can be omitted if the parent type and 
> all subtypes are declared in the same `.java` file.

#### @Permits

If it's not possible to implicitly order the subtypes according to the previous section
(in which case Rekex will report an error), or if such ordering is deemed unreliable,
you can use `@Permits` to list the subtypes in an explicit order.

    @Permits({JsonObject.class, JsonArray.class, JsonPrimitive.class})
    interface JsonValue{}

The parent type is not required be `sealed` if it's annotated with `@Permits`.

> The best practice is probably to define all types in one `.java` file,
> omit the `permits` clause, list subtypes in `@Permits`.

## Concatenation Rule as Constructor

Consider a member field in a Json object, which has the syntactic form of 
a Json string, followed by ":", followed by a Json value.
This can be expressed as a constructor

    record Member(JsonString name, Colon colon, JsonValue value){}

Note that this record class has one public constructor, the implicit *canonical constructor*.
`Colon` is assumed to be a datatype that matches `":"`, which we will talk about later.

To Rekex, the datatype `Member` corresponds to a concatenation rule of 3 subrules
corresponding to the parameter datatypes of the constructor.
At runtime, the parser will try to match the input against the subrules in sequence;
if successful, 3 arguments are instantiated which are fed to the constructor 
to instantiate a `Member` instance.

> The following sections will introduce more ways to represent alternation and concatenation rules;
> however, `sealed` types and canonical `record` constructors are the recommended way. 

## @Ctor

If there are more than one public constructors, you can annotate one of them with `@Ctor`
to tell Rekex to use that particular one to derive the grammar rule for the datatype. 
For example,

    record Member(JsonString name, JsonValue value) 
    {
        @Ctor public
        Member(JsonString name, Colon colon, JsonValue value) {
            this(name, value);
        }
    }

Here, the canonical constructor does not store the colon in the record,
because the colon is irrelevant in the "semantic" structure.
A second public constructor is declared with `@Ctor` to express the "syntactic" structure. 

### concatenation rule as factory method

Instead of a constructor, you can declare a public **static method** with `@Ctor`
for the same purpose. The method return type must be a subtype of the datatype. 

    record Member(JsonString name, JsonValue value) 
    {
        @Ctor public static 
        Member ctor(JsonString name, Colon colon, JsonValue value) {
            return new Member(name, value);
        }
    }

Such static factory methods are more flexible than constructors in many cases.

### "Ctor"

We'll use the term **ctor** to refer to public constructors or public static methods
that are annotated with `@Ctor`, or, the sole public constructor of a datatype
(if the datatype contains no explicit `@Ctor` constructors/methods,
but does contain a single public constructor).

A ctor with an empty parameter list is allowed, and it corresponds to the ε-production rule,
matching the empty string.

Note that Java automatically introduces a default constructor in some cases

    class Epsilon{} // it has a default constructor `Epsilon()`

To avoid accidents, Rekex does not recognize it as a ctor,
even though it's the sole public constructor of the datatype.
If a zero-arg constructor is 
*intended* to be a ctor, it must be explicitly annotate with `@Ctor`.


### Thinning the parse tree

A ctor's signature reflects the full syntactic structure of a production rule;
the ctor's body however can return any object of the datatype.
This enables us to construct a parse tree with only the data we are interested in,
instead of construct a full syntax tree containing all syntactic nodes.

As an example of aggressive thinning, suppose we want to write a calculator for expressions
like `2*(3+4)`. We'll declare datatypes like `Term`, `Factor`, etc. corresponding to grammar rules;
however, each datatype only needs to store the numeric value of the subexpression;
evaluations are done in ctors' bodies along the way; the parse tree under construction 
is never more than one level deep.

On the other hand, thinning may not always be a good idea. 
The first example of `Member` looks so much simpler and nicer than the other two.
Most often it's not a big deal to produce a bloated parse tree first,
then immediately transform it to a desired form in a followup step.

## Alternative Ctors

Multiple ctors can be declared in a datatype (as constructors or static methods, or both)

    interface Foo{
        @Ctor public static FooA fooA(...){...}
        @Ctor public static FooB fooB(...){...}
    }

The datatype `Foo` corresponds to an alternation rule of two subrules,
each is a concatenation rule derived from the ctor.
In this example we list all syntactic rules for `Foo` in the body of `Foo`, 
instead of in different subtypes.
It is also possible to hoist ctors to a central "ctor catalog" class
which we will discuss later.

A datatype having only one ctor is a special case of this section,
corresponding to an alternation rule of one subrule. 

### Order of Ctors 

Order of subrules matters.
Rekex will try to establish an implicit order among the ctors 
based on their declaration order in the source file.
This usually works and you don't need to worry about it.
If it doesn't work, Rekex will report an error, and you'll
have to specific the order by `@Ctor(1)`, `@Ctor(2)`, etc. 


## Repetition Rule as `List<E>` or `E[]`

A repetition rule `Foo*` matches subrule `Foo` zero or more times sequentially. 
Naturally, the corresponding Java datatype is `List<Foo>` or `Foo[]`. 

Assuming `JsonChar` is a datatype that matches a logical Json character, 
which could be a single input char or an escape sequence.
A Json string contains zero or more logical characters enclosed in quotes.

    record JsonString(Quote qL, List<JsonChar> chars, Quote qR) implements JsonPrimitive{}

> Note - Repetition rules could be encoded as alternation and concatenation rules, e.g. 
  <br> `Foos = Foo Foos | ε` . <br> 
  Some grammar authors prefer this approach; and you can do the same in Rekex as well. 
  Nevertheless, Rekex natively supports repetition rules because they are very common in grammars. 

You can specify a quantifier for the repetition with `@SizeLimit(min,max)`, or equivalently, with
`@AtLeast(n)`, `@AtMost(n)`, `Size(n)`.

        @AtLeast(1) List<Foo> foo

This is an example of "annotated type", which further constrains allowed values.
The annotation applies to the type `List<Foo>`, not to the parameter `foo`, 
see [JLS-9.7.4](https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.4). 
More complex annotated types can be constructed for example

        List< @AtMost(3)List<Foo> >

This means a repetition of 0 or more `X`,
where `X` is a repetition of `Foo` 0 to 3 times.

Array types `E[]` are supported too, gratuitously; `E` can be a primitive type.

Annotations on array types are somewhat strange, 
see [JLS-9.7.4](https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.4).
For example

        @Ch("abc")char @Size(4)[]

`@Size(4)` applies to the array type, limiting the size of the array;
while `@Ch("abc")`, explained later, applies to the component type `char`, 
constraining an individual element in the array. 
The equivalent `List` form is

        @Size(4)List< @Ch("abc")Character >


## Lookaheads as `Peek<E>` and `Not<E>`

Positive and negative lookaheads, 
a.k.a `and-predicate` and `not-predicate` in PEG,
are syntactic predicates that do not consume any input.
In Rekex they are expressed in datatypes `Peek<E>` and `Not<E>`.

For example, rule `A` is defined as concatenation of `B C`, except in the case of `B C1`. 
We need a negative lookahead at the position after `B` to exclude `C1`.

    record A(B b, Not<C1> nc1, C c){}

> `Not<C1>` may confuse a casual observer though, 
  as if `nc1` could be one of the many values outside type `C1`.
  You may instead use the equivalent form `Peek<Not<C1>>` 
  which expresses more verbally that it is a negative lookahead. 

## Generic Datatypes - `Either<A,B>`, `Opt<E>`, etc.
                          
Datatypes and ctors can be generic with type parameters;
Rekex will perform necessary type inference and substitutions. 
For example, you can declare a generic type

    record Def<N, V> (N name, Colon colon, V value) {}

when Rekex encounters a datatype `Def<Foo,Bar>`, it will derive the grammar rule 
as concatenation of `(Foo, Colon, Bar)`.

With this mechanism, Rekex ships with a few generic helper datatypes. 
They are not native to Rekex core, and you can easily define your own.
The helper datatypes include

- `Either<A,B>` - matches either A or B, as an ad-hoc sum type. 
    > We don't always want to, and sometimes we cannot,
      impose a common supertype for A and B. 
      `Either<A,B>` is a quick and direct way to express rule `A|B`
- `Alt2<A,B>` etc. - ad-hoc sum types
- `Seq2<A,B>` etc. - ad-hoc product types
- `Opt<E>` - optionally matches E, i.e. `E?`
- `OneOrMore<E>` - maches E one or more times, i.e. `E+`
- `SepBy<T,S>` - zero or more `T`, separated by `S`, i.e. `(T (S T)*)?`
- `SepBy1<T,S>` - one or more `T`, separated by `S`, i.e. `T (S T)*`

For example, a Json array contains zero or more values separated by comma `","`
which can be expressed as `SepBy<JsonValue,Comma>`

    record JsonArray(
        @Ch("[")char bL, SepBy<JsonValue,Comma> values, @Ch("]")char bR
    ) implements JsonValue{}

`JsonObject` is similar with `SepBy<Member,Comma>` .


## Tokenizer with @Regex

Finally, at the bottom level of a grammar, we need rules to match input characters (i.e. terminals).
This is known as lexing or tokenization. Rekex does it by datatypes annotated with regular expressions.

### @Regex on String

A `String` type annotated with `@Regex` matches strings that match the regular expression. 
For example, to match one or more digits

        record Digits( @Regex("[0-9]+")String str ){}

The regular expression is of the Java dialect, defined in
[java.util.regex.Pattern](https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/regex/Pattern.html) .


### Equivalents of @Regex 

Other annotations can be used in place of `@Regex` 
if they are convertible to `@Regex` through 
[AnnoMacro](./AnnoMacro.md). You can define your own macros, but most likely you'll only need
`@Ch` and `@Str` provided by Rekex.

    @Ch("abc")  // matches "a" or "b" or "c"

    @Ch(range={' ', 0x10FFFF}, except="\"\\")

    @Str("true")  // matches literal string "true"

    @Str({"true", "false"})  // matches literal string "true" or "false"

See more details [here](./RegexAnno.md).

### @Regex on character

For a single unicode character, use datatypes 
`char, int, Character, Integer`, annotated with `@Regex` or equivalent. 
For example

    @Ch("+-*/")char

    @Ch(range={'A','Z'})int []

    List< @Ch(range={'A','Z'})Integer >

Note that Java regular expression works at the unit of "code point", a 32-bit value.
If the regex matches the input, it must match exactly 1 code point.
If the datatype is `char` or `Character`, the code point matched
must not exceed `0xFFFF`.

### @Regex on Enum

You can apply @Regex or equivalent on values of an `enum` type; 
the enum type corresponds to an alternation rule
with one subrule for each value, in their declaration order.

    enum JsonBool implements JsonPrimitive
    {
        @Str("true") TRUE,
        @Str("false") FALSE,
    }
    enum JsonNull implements JsonPrimitive
    {
        @Str("null") NULL
    }

Don't hesitate to define singleton enums with only one value; they are quite common.

    enum Comma{ @Ch(",")I }

> We could also define singleton enums for tokens like `"["`. 
The benefit is that we clearly separated tokenization from parsing; the rest of the grammar 
looks cleaner. On the other hand, since `"["` appears only once in our grammar,
some people may prefer to use `@Ch("[")char` on the spot, 
avoiding introducing a new datatype for it.

### RegExp - structured regular expression

There is no objective boundary between tokenizer and parser.
Depending on your level of tolerance with regular expression, 
you may use one complex regex to match a token in one step,
or you may use simpler ones to match internal components of the token,
and define the token in terms of these components. 

There is good news. Complex regular expressions can be composed with the
[RegExp](./RegExp.md) library, in a very elegant and readable way.
See the [example](RegexAnno.md) of how to define a `@Regex` equivalent
annotation with the RegExp API.

### Whitespace handling

The Json grammar, like most grammars, allows whitespaces between most tokens.
The best way to handle them is to consume trailing whitespaces after each token.

Here's how it's achieved in Rekex.
An `@Regex` can specific a `group` attribute, for example

    @Regex(value="(true)\\s*", group=1)

This regex matches `"true"` followed by zero or more whitespaces. 
The `group` attribute indicates that only group#1 is the effective payload 
we are interested in. If this `@Regex` is applied to a `String`, 
the grammar rule will match and consume `"true"` with trailing whitespaces, 
but the content of the `String` will only contain `"true"`.

We need a simpler macro for this purpose.
In your code, define your own annotation macro `@Token`

    static final String wsChars = " \t\n\r";  // whitespace chars allowed by Json

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface Token
    {
        String[] value();
        AnnoMacro<Token, StrWs> toStrWs = StrWs.Macro.of(Token::value, wsChars);
    }

Don't mind the ugly details, just copy this `@Token` definition into your code,
replace `wsChars` with whitespaces of your grammar.
`@Token` matches any of the string literals in `value()`
followed by zero or more chars in `wsChars`.

Next, review all existing tokenizers; if trailing whitespaces are allowed after a token, 
use `@Token` instead of `@Ch` or `@Str`.

    enum JsonBool implements JsonPrimitive
    {
        @Token("true") TRUE,
        @Token("false") FALSE,
    }

    enum Comma{ @Token(",")I }

    record JsonArray( @Token("[")char bL, ... ) ...

Be careful though. For example, the starting quote of a Json string cannot be a `@Token`,
because whitespaces after the quote are characters in the string and cannot be skipped.
(Maybe "Token" isn't a great choice of name; pick your own name.)

We'll also need to match optional whitespaces in some other places,
notably leading whitespaces in inputs, which `@Token` doesn't address.
Define a datatype for optional whitespaces and insert it wherever it's allowed. 

    // zero or more whitespaces
    enum OptWs{ @Token("")I }

    // skip leading whitespaces in input
    record JsonInput(OptWs leadingWs, JsonValue value){}


## Semantic Predicate

Some times it is difficult or impossible to express a rule in a syntactic way,
when we have to resort to Java code to validate the input.
For example, let's say we need a rule to match *prime numbers*.
Syntactically, the best we can do is to match zero-or-more digits;
whether or not it is actually a prime number can only be tested in Java code,
by a *semantic predicate*.

A ctor can indicate that it contains semantic predicates by declaring 
possible failure types as exception types in the `throws` clause.
If an exception of one of those types is thrown when invoking the ctor,
we say that the ctor fails a semantic predicate.


        @Ctor public static 
        CoolNumber prime(@Regex("[0-9]+")String str) throws SomeException
        {
            int num = Integer.parseInt(str);
            if(!isPrime(num))
                throw new SomeException("not a prime: " +str);
            return new CoolNumber(num);
        }

If a ctor fails a semantic predicate, 
the corresponding rule fails to match the input.
However, this is not a fatal error; 
the parser does not stop immediately; instead,
alternative rules will be explored if there's any.
For example, 
if `prime()` fails the semantic predicate,
the next ctor for `CoolNumber`  (if there is one) 
will be tried which may successfully match the input.

A semantic predicate can be considered a *lookbehind* rule
that tests the arguments to a ctor after they have matched the input.
                            
All failure types must be explicitly listed 
in the `throws` clause, including unchecked exception like`IllegalArgumentException`. 

If a ctor throws an `Exception` that's not declared in the `throws` clause,
it is "unexpected", and the parser stops immediately
with a `Fatal` result, with the exception as the cause.
In the example above, `parseInt(str)` could throw `NumberFormatException`
for a very long `str`; since the exception type is not declared,
it would cause a `Fatal` error.


If a ctor throws an `Error`, it will not be caught or handled by the parser.




## PegParser

Once the datatypes are defined, we can create a 
[PegParser](../rekex-parser/src/main/java/org/rekex/parser/PegParser.java)
for the *root type*

    PegParser<JsonInput> parser = PegParser.of(JsonInput.class);

The root type can be any datatype in the grammar. 
For example, if inputs are expected to be always Json objects

    PegParser<JsonObject> parser = PegParser.of(JsonObject.class);

Even "small" datatypes like `JsonChar` can be root types.
We can unit test our grammar piece by piece this way.

The method `PegParser.of(rootType)` generates and compiles a Java source file 
under the `/tmp` directory. 
For more options of how a parser is built, use 
[PegParserBuilder](../rekex-parser/src/main/java/org/rekex/parser/PegParserBuilder.java)

    var builder = new PegParserBuilder()
        .rootType(JsonValue.class)
        .packageName("com.example")
        .className("MyJsonParser")
        .outDirForJava(Paths.get("src/main/java"));

    PegParser<JsonValue> parser = builder.build();

We can print a text version of the grammar for inspection

    var grammar = builder.grammar();
    System.out.println( grammar.toText() );

The generated Java source file can be placed in our main src directory
so that we can instantiate the parser directly.

    static final PegParser<JsonValue> parser = new MyJsonParser();    

A `PegParser` is stateless, safe to be shared and invoked concurrently.
Meanwhile, the constructor is lightweight, 
so don't refrain from creating new instances on demand either.

## ParseResult

To invoke a parser against an input

    ParseResult<Foo> result = parser.parse(input); // or parse(input, start, end)
    System.out.println(result);

> In the current version of Rekex, only `CharSequence` inputs are supported.

There are 4 subtypes of
[ParseResult](../rekex-parser/src/main/java/org/rekex/parser/ParseResult.java)
: `Full, Partial, Fail, Fatal`

- `ParseResult.Full<Foo>` - the parser matched the input from start to end.
    ```                                                        
        if(result instanceof ParseResult.Full<Foo> full){
            Foo foo = full.value();
        }
    ```
  A convenience method `parser.matchFull(input) -> Foo` is also provided.

- `ParseResult.Partial<Foo>` - the parser matched the input from start to a `position`, `position < end` . 
    ```                                                        
        if(result instanceof ParseResult.Partial<Foo> partial){
            Foo foo = partial.value();
            int remaining = end - partial.position();
        }
    ```
  This can be used to accept an input that starts with a Foo, followed by something else 
  (for example, more Foos). The `position` should become the new start to parse the rest of the input.

- `ParseResult.Fail<?>` - the parser failed to match the input from start.
  The result contains a `position` in the input that's likely causing the failure, 
  usually the farthest position that the parser has advanced to. 
  The result also contains the parse stack leading to the position;
  each node in the stack contains the start position from which a rule was being tried,
  and its datatype. The stack can be investigated to create a better error message.
  ```
        if(result instanceof ParseResult.Fail<?> fail){
            System.out.println(fail.message()+" at "+fail.position());
            System.out.println(fail.stack().get(0));
        }
  ```
  
- `ParseResult.Fatal<?>` - the parser encountered an error. 
  Often it's because a ctor threw an `Exception` that's not declared in the `throws` clause,
  which stopped the parser immediately. 
  The result also contains the position and the parse stack when the error occurred.
  ```
        if(result instanceof ParseResult.Fatal<?> fatal){
            Exception cause = fatal.cause();
        }
  ```

Positions in the input can be translated into line and column numbers by `LineCounter`.

        var lc = new LineCounter(input);
        System.out.printf("line=%d, col=%d", lc.line(position), lc.column(position));

## Ctor Catalog

In previous examples, ctors of a datatype are declared in the body of the datatype.
In some cases, we don't want to, or cannot, add those methods there. 
Instead, we can declare ctors in a central class, the "ctor catalog" class.
Another reason to do this is to place all syntax rules in one place,
so that it's easier to review the grammar.

    public class CtorCatalog 
    {
        public FooA fooA(...){...}
        public FooB fooB(...){...}

        public Bar bar(List<Foo> foos){...}
        ...
    }

    PegParser<Bar> parser = new PegParserBuilder()
        .rootType(Bar.class)
        .catalogClass(CtorCatalog.class)
        .build(new CtorCatalog());

All `public` methods decalred in the catalog class are considered ctors. 
Do not declare public methods that are not intended as ctors.
`@Ctor` and `static` are allowed, but not required.

An instance of the catalog must be provided to the parser constructor.
The instance may be invoked concurrently and must be thread-safe.
You may have different instances to provide different runtime behaviors of ctors.

To find the ctors for a datatype, Rekex first searches the catalog for ctors
with a compatible return type.
If not found, Rekex searches the class body of the datatype.
If not found there either, Rekex searches subtypes of the datatype.




## References

- [Specification](./Spec.md)

- [AnnoMacro](./AnnoMacro.md)

- [@Regex and equivalents](./RegexAnno.md)

- [RegExp - structured regular expression](./RegExp.md)

- [PEG - Parsing Expression Grammar](https://bford.info/pub/lang/peg.pdf)

Comments? Questions? Bugs? Contributing? 
Please use the [Issue Tracker](https://github.com/zhong-j-yu/rekex/issues). 

----
*Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.*
