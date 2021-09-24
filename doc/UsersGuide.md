
# Rekex User's Guide

Rekex is a 
[PEG](https://en.wikipedia.org/wiki/Parsing_expression_grammar) 
parser generator for Java 17.
The distinguishing feature of Rekex is that
grammars are modeled in user-defined datatypes.
See [Introduction](./Intro.md) for a more detailed explanation of the design.

Maven dependency:

        <dependency>
            <groupId>org.rekex</groupId>
            <artifactId>rekex-parser</artifactId>
            <version>1.1.0</version>
        </dependency>

A quick example for a parser that matches strings of 0s and 1s:

        // binary = bit*
        // bit = "0" | "1"
                          
        record Binary(List<Bit> bits){}

        enum Bit{
            @Ch("0") v0,
            @Ch("1") v1,
        }

        PegParser<Binary> parser = PegParser.of(Binary.class);
        Binary result = parser.matchFull("0101");
        print(result.bits().get(3));



# PART I -- Grammar as Algebraic Datatypes

A PEG grammar can be expressed as a collection of algebraic datatypes,
*i.e.* `sealed`, `record`, `enum` types.
A parser can be generated for this grammar, which outputs parse trees
that are statically-typed in these user-defined datatypes.

## PEG

[Parsing Expression Grammar](https://en.wikipedia.org/wiki/Parsing_expression_grammar)
(PEG) is similar to Content-Free Grammar (CFG)
with two extra features: ordered choice and lookahead. 
It is easier to use PEG to describe language grammars in many applications.

We'll use the [Json Grammar](https://www.json.org) as the main example in this document.
The official grammar is a CFG; we'll treat it as a PEG that's trivially equivalent. 

A PEG contains *grammar symbols*, such as `value, object, member`.
Each symbol is associated with a *grammar rule*.
Rules can take the following forms:
- ordered choice: `value ← object / array / ...`
- sequence: `member ← string colon value` 
- repetition: `digits ← digit*`
- lookahead: positive `p ← &e`, negative `n ← !e`


## Grammar Symbols as Datatypes

In Rekex, every *symbol* in the grammar is represented by a unqiue *datatype* in Java.
During a parsing process, an input *phrase* that matches a symbol
will be represented by a *datanode* of the corresponding datatype.
There is a *root datatype* that represent the *start symbol*;
at the end of a successful parsing process, a datanode in the root datatype
is produced to represent the input *sentence*.

For Json, we'll need to define datatypes of `JsonValue, JsonObject, JsonArray, JsonString, ...` etc.
to represent corresponding grammar symbols.
If we choose `JsonValue` to be the root datatype, 
the parser will output a `JsonValue` object for a valid Json input. 

We don't necessarily follow the source grammar verbatim.
For example, we may introduce a `JsonBool` datatype
that has no counterpart in the source grammar,
or we may use a generic datatype like `List<Digit>`
to represent an in-line rule.
It's understood that the source grammar is refactored with more symbols introduced
to maintain the one-to-one correspondence between symbols and datatypes.

All datatypes must be `public`.
Any Java type can be chosen to represent a symbol, including primitive types.

## Grammar Rules

Every grammar symbol is associated with exactly one grammar *rule*,
often in relationship to other symbols.
Correspondingly, every datatype is associated with a grammar rule,
often in relationship to other datatypes.
For a quick peek how rules can be represented in Rekex: 

- choice and sequence rules as subtypes and constructors/methods
- repetition rules as `List<E>` or `E[]`
- lookaheads as `Peek<E>` and `Not<E>`
- terminal matching with `@Regex` annotations


## Choice Rule as Subtypes

`JsonValue` is a choice of `JsonObject, JsonArray, ...` etc.
Naturally, this relationship can be represented in a type hierarchy:

    sealed interface JsonValue
        permits JsonObject, JsonArray, JsonPrimitive{}

    sealed interface JsonPrimitive extends JsonValue 
        permits JsonString, JsonNumber, JsonBool, JsonNull{}

Notice that we choose to introduce `JsonPrimitive, JsonBool` in our datatypes 
which aren't in the original grammar.

▶ A `sealed` parent type corresponds to a choice rule of its direct subtypes.

### Order of subtypes

In PEG, choices are ordered; therefore we need to order the subtypes as well. 

Unfortunately, Java's reflection API does not define an order on the subtypes in a `permits` clause.
Rekex will try to establish an implicit order 
among subtypes based on their line numbers in the source file; 
this works if they are declared in the same `.java` file,
and the file is compiled with debug information enabled 
(which is the default behavior of `javac`).

We recommend that you declare all relevant datatypes in one `.java` file;
the declaration order of these types should be consistent with the order required by the grammar.
Preferably they are nested in a top level `public interface`, so that they are implicitly `public` as well;
this document assumes that datatypes are implicitly `public`.

> Note that the `permits` clause can be omitted if the parent type and 
> all subtypes are declared in the same `.java` file.

#### @Permits

If the implicit order can not be established
(in which case Rekex will report an error), or if the implicit order is considered unreliable,
you can use `@Permits` to list the subtypes in an explicit order.

    @Permits({JsonObject.class, JsonArray.class, JsonPrimitive.class})
    interface JsonValue{}

The parent type is not required be `sealed` if it's annotated with `@Permits`.


## Sequence Rule as Constructor

The grammar rule `member ← string colon value` can be expressed as 

    record Member(JsonString name, Colon colon, JsonValue value){}

Note that this record class has one public constructor, the implicit canonical constructor.

▶ A datatype with a single public constructor corresponds to a sequence rule
of the parameter datatypes.



## Repetition Rule as `List<E>` or `E[]`

A repetition rule `Foo*` matches subrule `Foo` zero or more times sequentially. 
Naturally, the corresponding Java datatype is `List<Foo>` or `Foo[]`. 
For example, a Json string contains zero or more logical characters enclosed in quotes.

    record JsonString(Quote qL, List<JsonChar> chars, Quote qR) implements JsonPrimitive{}

> Note - Repetition rules could be encoded as choice and sequence rules, e.g. 
  `Foos = Foo Foos | ε` . <br> 
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
Annotations on array types are kind of strange, 
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
a.k.a `and-predicate (&e)` and `not-predicate (!e)` in PEG,
are syntactic predicates that do not consume any input.
In Rekex they are expressed in datatypes `Peek<E>` and `Not<E>`.

For example, `A` is a sequence of `B C`, except in the case of `B C1`. 
We need a negative lookahead at the position after `B` to exclude `C1`.

    record A(B b, Not<C1> nc1, C c){}

> `Not<C1>` may confuse a casual observer though, 
  as if `nc1` could be one of the many values outside type `C1`.
  You may instead use the equivalent form `Peek<Not<C1>>` 
  which expresses more verbally that it is a negative lookahead. 

## Generic Datatypes - `Either<A,B>`, `Opt<E>`, etc.
                          
Datatypes can be generic with type parameters;
Rekex will perform necessary type inference and substitutions. 
For example, you can declare a generic type

    record Def<N, V> (N name, Colon colon, V value) {}

when Rekex encounters a datatype `Def<Foo,Bar>`, it will derive the grammar rule 
as a sequence of `(Foo, Colon, Bar)`.

With this mechanism, Rekex ships with a few generic helper datatypes. 
They are not native to Rekex core, and you can easily define your own.
The helper datatypes include

- `Either<A,B>` - matches either A or B, as an ad-hoc sum type. 
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


## Token Datatypes with @Regex

At the leaf level of a grammar, we need rules to match input characters (i.e. terminal symbols).
This is known as lexing or tokenization. Rekex does it by datatypes annotated with regular expressions.

### @Regex on String

A `String` type annotated with `@Regex` matches phrases with the regular expression. 
For example, to match one or more digits

        record Fraction( Dot dot, @Regex("[0-9]+")String digits ){}
                                  ---------------------- 

The annotated type `@Regex("[0-9]+")String` is a *token datatype*.
A token datatype corresponds to a (synthetic) grammar symbol for which the rule
is defined by a regular expression on terminal symbols.

### Equivalents of @Regex 

`@Regex` can contain any regular expression of the Java dialect, defined in
[java.util.regex.Pattern](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html) .

Other annotations can be used in place of `@Regex` 
if they are convertible to `@Regex` through 
[AnnoMacro](./AnnoMacro.md). You can define your own macros, but most likely you'll only need
`@Ch` and `@Str` shipped with Rekex.

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
For single character datatypes,
if the regex matches the input, it must match exactly 1 code point;
if the datatype is `char` or `Character`, the code point matched
must not exceed `0xFFFF`.

### @Regex on Void

If you don't care about the characters that matched a regex, use the `Void` type.
It's typically for tokens that aren't important in the semantic structures,
for example, the brackets around a Json array

        record JsonArray(@Ch("[")Void bL, SepBy<JsonValue,Comma> values, @Ch("]")Void bR)

### @Regex on Enum values

You can apply @Regex or equivalent on values of an `enum` type; 
the enum type corresponds to a choice rule
with one subrule for each value, in their declaration order.

    enum JsonBool implements JsonPrimitive
    {
        @Str("true" ) TRUE,
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
The best way to handle them is to consume trailing whitespaces after such tokens.

Here's how it's achieved in Rekex.
An `@Regex` can specific a `group` attribute, for example

    @Regex(value="(true)\\s*", group=1)

This regex matches `"true"` followed by zero or more whitespaces. 
The `group` attribute indicates that only group#1 is the effective payload 
we are interested in. If this `@Regex` is applied to a `String`, 
the grammar rule will match and consume `"true"` with trailing whitespaces, 
but the content of the `String` will only contain `"true"`.

We need a simpler macro for this purpose.
In your code, define your own annotation macro `@Word`

    static final String wsChars = " \t\n\r";  // whitespace chars allowed by Json

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface Word
    {
        String[] value();
        AnnoMacro<Word, StrWs> toStrWs = StrWs.Macro.of(Word::value, wsChars);
    }

Don't mind the ugly details, just copy this `@Word` definition into your code,
replace `wsChars` with whitespaces of your grammar.
`@Word` matches any of the string literals in `value()`
followed by zero or more chars in `wsChars`.

Next, review all existing tokenizers; if trailing whitespaces are allowed after a token, 
use `@Word` instead of `@Ch` or `@Str`.

    enum JsonBool implements JsonPrimitive
    {
        @Word("true" ) TRUE,
        @Word("false") FALSE,
    }

    enum Comma{ @Word(",")I }

    record JsonArray( @Word("[")char bL, ... ) ...

    @Word({"+", "-"}) char op

Be careful not to apply `@Word` on tokens that may be followed by 
significant whitespaces that cannot be ignored,
for example, the starting quote of Json strings. 

We'll also need to match optional whitespaces in some other places,
notably leading whitespaces in inputs, which `@Word` doesn't address.
Define a datatype for optional whitespaces and insert it wherever it's allowed. 

    // zero or more whitespaces
    enum OptWs{ @Word("")I }

    // skip leading whitespaces in input
    record JsonInput(OptWs leadingWs, JsonValue value){}


## ParseInfo

`ParseInfo` can be inserted anywhere in a sequence rule,
with no effect on rule matching. 
It contains information about input regions that matched the rule and subrules.

        record FooBar(Foo foo, Bar bar, ParseInfo info)
        {
            void test()
            {
                println(info.text()); // FooBar
                println(info.of(foo).text());  // Foo
    
                assert info.start() == info.of(foo).start();
                assert info.end() == info.of(bar).end();
            }
        }



## PegParser

Once all datatypes are defined, we can create a 
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

    PegParser<JsonValue> parser = new PegParserBuilder()
        .rootType(JsonValue.class)
        .packageName("com.example")
        .className("MyJsonParser")
        .outDirForJava(Paths.get("src/main/java"))
        .logGrammar(System.out::println)
        .build();

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




# PART II -- AST Construction

In *PART I*, we define algebraic datatypes to faithfully mirror the structure of the grammar.
The parser outputs *parse trees*, a.k.a. *concrete syntax trees*,
that retain all syntactic structure and information from inputs.

Usually, a parser application needs to produce some other kinds of results from inputs. 
We'll refer to the desired results as *abstract syntax trees* (ASTs),
in any sense of abstractness. 
For example, a calculator application may only need to produce a numeric value
for a math expression; the numeric value is the AST. 

If we have a parser that outputs parse trees, we can transform parse trees to any ASTs
in separate stages after the parsing process. 
This is a clean and versatile approach -- 
the grammar is transliterated into algebraic datatypes;
the parser produces parse trees from inputs;
then transformers produce ASTs from parse trees.

However, a programmer may have good reasons to expect a specific kind of AST
directly out of the parser. Maybe that's the only requirement of the application.
Maybe that's for removing useless syntactic information from datanodes.
In any case, we need to integrate AST productions in the parsing process.

## Ctor
       
Previously, we used the canonical constructor of a record class
to express the sequence rule for the datatype.

    record Member(JsonString name, Colon colon, JsonValue value){}

The canonical constructor actually servers 3 purposes at the same time:
- the list of parameter types defines fields in the datatype
- the list of parameter types defines the grammar rule for the datatype
- the body instantiates a datanode,
  by simply storing the parameters in the fields

If we want to design the datanode to store information differently,
for example, to store only `(name,value)` but not `colon`

    record Member(JsonString name, JsonValue value){ ... }

we need to provide an explicit **ctor** for the datatype,
which is a constructor/method where
- the return type is the datatype
- the list of parameter types defines the grammar rule for the datatype
- the body returns a datanode of the datatype

An explicit ctor can be either a public constructor marked by `@Ctor`


    record Member(JsonString name, JsonValue value) 
    {
        @Ctor public 
        Member(JsonString name, Colon colon, JsonValue value) {
            this(name, value);
        }
    }

or a public `static` member method marked by `@Ctor`

    record Member(JsonString name, JsonValue value) 
    {
        @Ctor public static 
        Member ctor(JsonString name, Colon colon, JsonValue value) {
            return new Member(name, value);
        }
    }

The second approach is recommended because factory methods are more flexible 
than constructors.

To give another example, `JsonNull` could be designed as

    interface JsonNull extends JsonPrimitive
    {
        JsonNull singleton = ...;
        
        @Ctor public static 
        JsonBool ctor( @Str("null")Void arg ){ return singleton; }
    }

▶ A ctor for a datatype defines a sequence rule of the parameter datatypes.

A ctor with an empty parameter list defines an ε-production rule,
matching the empty string.

## Multiple Ctors

Multiple ctors can be declared for a datatype.
For example, we may choose to design `JsonBool` as

    interface JsonBool extends JsonPrimitive
    {
        boolean value();
        
        @Ctor public static 
        JsonBool trueV( @Str("true")Void arg ){ return ... }

        @Ctor public static 
        JsonBool falseV( @Str("false")Void arg ){ return ... }
    }

Here, `JsonBool` has two ctors, with different parameter types
to represent two different rules.

> All ctors for a datatype must have a return type
> that's exactly the same as the datatype.

▶ A datatype with *N* ctors corresponds to a choice rule
  of *N* subrules, each subrule defined by a ctor.


### Order of Ctors

Order of subrules matters.
Rekex will try to establish an implicit order among the ctors
based on their declaration order in the source file.
This usually works and you don't need to worry about it.
If it doesn't work, Rekex will report an error, and you'll
have to specific the order by `@Ctor(1)`, `@Ctor(2)`, etc.


## Ctor Catalog

Sometimes we don't want to, or can't, define ctors for a datatype
within the class body of the datatype. 
Maybe we want to keep the datatype declaration clean,
free of syntactic rules.
Maybe the datatype is from 3rd party that we cannot modify.
Maybe the datatype doesn't even have a class body -- 
primitive datatypes can be chosen to represent grammar symbols.

Ctors for a datatype can be declared in a central *ctor catalog* instead

    public class JsonCtors 
    {
        public Member member(JsonString name, Colon colon, JsonValue value)
        {
            return new Member(name, value);
        }

        public JsonBool trueV(...){...}
        public JsonBool falseV(...){...}

        ...
    }

*All `public` methods declared in the catalog class are considered ctors.*
Do not declare public methods that are not intended as ctors.
`@Ctor` and `static` are allowed on ctors, but not required.

If a ctor catalog is used to define some ctors,
the catalog class must be provided to the parser builder,
an instance of the catalog must be provided to the constructor of the parser.

    PegParser<JsonValue> parser = new PegParserBuilder()
        .rootType(JsonValue.class)
        .catalogClass(JsonCtors.class)
        .build(new JsonCtors());

    // or
    PegParser<JsonValue> parser = PegParser.of(JsonValue.class, JsonCtors.class, new JsonCtors());  


The catalog instance may be accessed concurrently and should be immutable;
it may contain constant data accessible to its ctors.
You may instantiate multiple parsers each with a different catalog instance
that influence ctors differently.


## Ctors as pure functions

Ctors should not cause side effects, nor should they depend on side effects.
The body of a ctor should depend only on its arguments,
and possible some constant data that remain the same throughout the parsing process.


## Datatype to Rule

Given any datatype, Rekex derives a grammar rule for it.
This often involves other datatypes. 
Given the root datatype, Rekex recursively derives 
all datatypes and rules for the entire grammar.

Since there are multiple ways the rule for a datatype
can be expressed, Rekex uses the following procedure
to derive the rule unambiguously:

1. If the ctor catalog contains one or more ctors for the datatype, 
  the rule is derived from these ctors.

2. If the class/interface of the datatype contains one or more ctors, 
the rule is derived from these ctors.

3. If the datatype is a `sealed` type, the rule is derived from its subtypes.

4. If the datatype contains a single public constructor,
  the constructor is the implicit ctor for the datatype.

Note that (3) can be interpreted as a list of implicitly ctors as well; 
if written explicitly:

        public JsonValue object(JsonObject obj){ return obj; }
        public JsonValue array(JsonArray arr){ return arr; }
        ...

Therefore, to derive the rule for a datatype is to find
the ctors for the datatype, and the ctors are found either
in the catalog, or within the datatype, 
or from subtypes, or from the single constructor.  

The [Specification](./Spec.md) defines this procedure more rigorously. 

## Refactor from concrete to abstract

It's probably a good idea to design a parser by starting
with simple algebraic datatypes that directly mirror the grammar.
Then we can incrementally refactor individual datatypes.
For example, we start with `record Member(JsonString,Colon,JsonValue){}`.
Now we want to discard `Colon` from the datatype.
We first add an explicit ctor to replace the implicit ctor

    public Member member(JsonString name, Colon colon, JsonValue value)
    {
        return new Member(name, colon, value);
    }

which is trivial and doesn't do anything useful.
But then we can safely refactor `Member` without breaking the grammar.
We can do this progressively until all datatypes,
particularly the root datatype, 
contain only the information we need from ASTs.


## Semantic Predicate

Some times it is difficult or impossible to express a parsing 
rule syntactically in grammar rules,
and we have to resort to Java code inside ctors to check the rule.

Imagine we are designing a calculator that supports functions and
variables. A phrase in the form of `a(b)` means a function call,
if `a` is a known function name. Unfortunately, if `a` is not a function
name but a variable name, we are required to interpret 
`a(b)` as implicit multiplication `a*(b)`.
This ambiguity cannot be resolved syntactically, 
if function names and variables names are not fixed.
Nor can we match `a(b)` with a more abstract construct
and delay the interpretation post parsing,
because `a(b)^c` could represent two different tree structures,
`(a(b))^c` vs `a*((b)^c)`, depending on the meaning of `a`.

We can solve the problem with a *semantic predicate*
on the ctor that matches function calls
                                   
    record CtorCatalog(Set<String> funcNames) // with config data
    {
        // name '(' arg ')'
        public FuncCall fc(Name name, PL pl, Expr arg, PR pr) throws NotFuncException
        {
            if(!funcNames.contains(name.text()))
                throw new NotFuncException(); // semantic predicate failure
            return new FuncCall(name, arg);
        }

When a phrase `a(b)` matches this rule 
(which is ordered before implicit multiplication in grammar definition),
the ctor is invoked; if the ctor throws an `Exception` that's explicitly
listed in the `throws` clause, the ctor fails a semantic predicate, 
the rule is vetoed. It doesn't stop the parser immediately;
the parser will go on attempting to match alternative rules.
A semantic predicate can be considered a *lookbehind* rule
that tests the arguments to a ctor after they have matched the input.

▶ A ctor contains semantic predicates if it has an explicit 
  `throws` clause. A ctor fails a semantic predicate if it
  throws an `Exception` that's declared in the `throws` clause.

Note that Rekex does not (officially) support *context-sensitive*
semantic predicates which depend on preceding phrases,
because that would require ctors with side effects.

If a ctor throws an `Exception` that's not explicitly declared,
it is considered a fatal failure, and the parser stops immediately.
For example, if there's no alternative interpretation for `a(b)`,
and `a` must be a function name, the ctor can be written as 

        public FuncCall fc(Identifier name, PL pl, Expr arg, PR pr)
        {
            if(!funcNames.contains(name.text()))
                throw new IllegalArgumentException(); // fatal failure

If a ctor throws an `Error`, the `Error` will not be caught or handled by the parser.


## Conclusion

In Rekex, grammar symbols are represented by user-defined datatypes;
grammar production rules are represented by ctor signatures;
input phrases are mapped to datanodes by invoking ctors. 
Grammar definition and AST construction are unified 
naturally as ordinary Java types and methods.
There are no scaffolding code; parsers are written in the minimum 
amount of code possible, and everything is nicely and statically typed.


## References

- [Examples](../rekex-example/src/main/java/org/rekex/exmple/parser)

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
