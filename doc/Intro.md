# Rekex, the simplest parser generator

Rekex is a PEG parser generator, targeting Java 17.
It introduces a new methodology for grammar definition and AST construction
that unifies the two aspects,
leading to the simplest, easiest, and most intuitive approach to writing parsers.

For a quick explanation, consider a production rule *A = B C* in a grammar. 
In Rekex, non-terminal symbols *A, B, C* are represented by user-defined 
datatypes `A, B, C`; a production rule *A = B C* is represented by
a function that computes `A` from `(B,C)`

        public A a(B b, C c){ return ...; }

A collection of such datatypes and functions precisely defines a grammar,
for which a parser can be generated that constructs AST nodes by invoking the functions.
The idea is so simple yet powerful. It's immediately obvious that
any context-free grammar can be defined this way,
and any type of ASTs can be constructed this way. 

It could get even simpler.
If the datatype `A` is designed to simply record `(B, C)`,
the production rule *A = B C* can be implied by the datatype definition itself

        record A(B b, C c){}

Assuming the programmer wants to produce parse trees in
user-defined datatypes,
this is the minimum amount of code that must be written anyway,
therefore it is objectively the simplest possible approach to parser generations.
                                                                
Before we get into more details of this new toy, 
let's look back at the old ones and
see what makes them more complicated.


## Conventional parser generators

A conventional parser generator, such as 
[ANTLR 4](https://github.com/antlr/antlr4/blob/master/doc/index.md), 
[Parboiled](https://github.com/sirthias/parboiled/wiki),
provides a DSL that's tailored for grammar definitions.
The programmer defines a grammar in this DSL,
from which a parser is generated that can recognize inputs and produce parse trees.
That sounds straightforward, and the introductory examples all look tidy and clean.

However, for most applications,
other than the parse trees built by the parser,
we need to produce some end results that we want.
There are mainly two ways to do it:

- Actions, written in the target programming language,
  can be hooked to the grammar structure in various places,
  reacting to parse events that are emitted in specific orders.
  Actions are necessarily *imperative*, operating on mutable variables
  for the purpose of producing the end result.
  A lot of careful plumbing is required to make it work correctly,
  and it's not gonna be pretty.

- The parse tree, produced by a parsing process, 
  can be transformed to something else in post-processing. 
  This requires the programmer to handle parse tree nodes,
  which are either weakly-typed, 
  or strongly-typed albeit in parser-generated datatypes
  (often requiring the grammar be augmented).
  Parboiled is of the first case; ANTLR the second.
  In either case, there's a fragile link between the structure of the grammar
  and the application code that depends on that structure.

Conventional parser generators are grammar-oriented,
with the primary concern on grammar definition and input parsing,
treating AST construction as a secondary concern that's glued on the primary. 
But for programmers, both are equally important.



## The mirror model 

A parser is essentially a function that maps input sentences (structured by the grammar)
into ASTs (structured by the datatypes).
We use the term "AST" loosely here to mean any kind of results, 
even as simple as boolean values.

Programmers don't think of a grammar as an abstract mathematical entity.
Rather, a grammar carries semantics within itself. 
A symbol in a grammar represents a kind of *things*. 
An input phrase that matches a symbol represents a thing of the kind. 
This sounds familiar to programmers because we represent things 
as values of datatypes in a programming language.
In fact, a context-free grammar looks *exactly* like a set of algebraic datatypes.
Therefore we can have two representations of the same things
that are mirror image of each other. 
As programmers, we'd rather work exclusively with the datatype representation.

A mirror model for parsing emerges naturally: every symbol *A* is mirrored by a datatype `A`;
every phrase that matches symbol *A* is mirrored by a datanode in datatype `A`. 
Every datanode depends on child nodes that mirror subphrases.
However, a datanode does not have to retain all child nodes;
it can retain just the information that's needed by its parent node;
the root node only needs to retain information that the programmer wants
out of the parsing process. 
Therefore, by defining datatypes for symbols along with functions that map 
child nodes to parent nodes, we get both grammar definition and AST construction. 


There are two major limitations to this model:

- *Context-sensitive semantic predicates* are impossible
  if there are no actions with side effects.
  This is rarely a concern except for languages that are
  designed to be difficult, such as C++.
- *Automatic error tolerance* is impossible
  if only correctly-typed ASTs can be constructed.
  This is a deal-breaker for applications that must
  make sense of the entire input as much as possible, 
  even if there are localized syntax errors. 

For most applications, these limitations aren't a problem,
and the mirror model works very well.
There is nothing new here, most programmers think and work in this model intuitively.
However, it's not so simple to practice it in conventional parser generators.
We need a new framework that directly supports the mirror model.


## Rekex

In Rekex, a grammar is expressed as a collection of Java datatypes and methods,
in a way that it's straightforward to translate between the two in either directions.

Non-terminal symbols in the grammar are represented by datatypes in Java,
in one-to-one correspondence.
There are *token datatypes* and *special datatypes* which we'll explain later;
the rest are *composite datatypes*.

A composite datatype represents a symbol that has one or more production rules
(that is, a symbol on the left-hand-side of some production rules).
Correspondingly, a composite datatype has one or more ctors.
A ***ctor*** is a method where the signature represents a production rule, 
and the body instantiates a datanode when the rule matches.
For example,

        public A a(B b, C c) throws E { ... }

- the return type `A` and parameter types `(B,C)` represents a production rule *A = B C*
- the `throws` clause lists possible types of *semantic predicate* failures
- the body either returns an instance of `A`, or raises a failure

There is no guarantee in which order ctors are invoked during a parsing process,
or even the number of times they are invoked.
Therefore a ctor should be a *pure function* that depends only on its arguments
(and possibly config data that remain constant during a parsing process).

For any composite datatype, Rekex finds its ctors either in a central *ctor catalog* class,
or within the class/interface that defines the datatype.
A composite datatype can be any Java type, including primitives,
as long as it has one or more ctors.

Given the *root datatype* that represents the *start symbol* of a grammar,
Rekex recursively discovers all datatypes and ctors for the entire grammar.
A parser is then generated for this grammar that constructs ASTs in the root datatype.

It is fair to say that there's unprecedented simplicity, clarity,
and elegance in this approach.
It is *trivial* to write a parser this way.
But wait, there's more.


## Implicit ctors

Often there are grammar rules like *A = A1 | A2* ,
and the meanings of the symbols lead us to design datatype `A` as the parent type of `A1, A2`,

        sealed interface A permits A1, A2{}

The grammar rule *A = A1 | A2*, which is really sum of two production rules, *A=A1, A=A2*,
can be expressed in ctors like

        public A a1(A1 a1){ return a1; }
        public A a2(A2 a2){ return a2; }

These ctors can be omitted. If there's no explicit ctors for a datatype `A`,
and `A` is a *sealed* type with subtypes `A1, A2`,
ctors like these are implicit declared for `A`.

Next, suppose symbol *A1* has one production rule *A1 = X Y*,
and datatype `A1` has the same form as 

        record A1(X x, Y y){}

it seems rather repetitive to have to declare the ctor like

        public A1 a1(X x, Y y){ return new A1(x, y); }        

This ctor can be omitted as well. If there's no explicit ctors for a datatype `A1`,
and `A1` has only one public constructor,
the constructor *is* the implicit ctor for `A1`.

In summary, variant rules could be expressed in `sealed` types,
and production rules in `record` types, without the need of explicit ctors.

## Grammar as algebraic datatypes

With implicit ctors, a context-free grammar can be
expressed entirely in `sealed` and `record` types,
i.e. *algebraic datatypes*.
This is not surprising, since both formalisms have the same forms.
It is by no coincidence that a production rule maps to a *product type* (`record`),
and a set of rules maps to a *sum type* (`sealed`).

To design a parser with Rekex, we could start by directly transliterating
the grammar into algebraic datatypes. This immediately gives us
a parser that outputs *parse trees*,
a.k.a. *concrete syntax trees*,
which contain all syntactical structure and data from input strings.
This could be the sole responsibility assigned to the parser
while other works are assigned to post-processing,
which are easy to do because the parse tree is statically-typed
in user-defined algebraic datatypes.
Different kinds of ASTs can be produced by different transformers.

If we do want a specific kind of "abstract" syntax trees directly out of the parser,
we need to refactor some datatypes,
either to discard some syntactic information,
or to store information in different formats.
We can incrementally refactor from the concrete to the abstract.
Say we started with `record A1(X,Y)`, and now we want it to become `record A1(Z)`;
we first introduce the
explicit ctor to keep the grammar rule for *A1*

        public A1 a1(X x, Y y){ return new A1(x, y); }

then we can safely refactor `record A1` without breaking grammar rules.
This engineering approach, working from parse tree to AST, has the advantage
that we can quickly and correctly model the grammar as a first step,
getting a useful parser with very little effort;
then we can improve on it progressively,
confident that correctness is maintained throughout refactoring processes.
It's easy to get started, and pleasant to move forward.


## Generic datatypes

Datatypes and ctors can be *generic* with type parameters;
Rekex performs type inference and substitutions whenever necessary.
This mechanism enables us to design generic datatypes for generic rule constructs,
for example, `Opt<E>` for *E?* , `Either<A,B>` for *(A | B)* ,
and `SepBy<T,S>` for *(T (S T)\*)?* .
Rekex ships with some commonly used generic datatypes;
users can easily define their own.
They are crucial for expressing grammar rules succinctly; for example

        public Integer integer(Opt<Sign> sign, OneOrMore<Digit> digits){ ... }

## PEG support

The general ideas explored so far can be applied to any grammar
that is mostly "context free" in form.
To support PEG (Parser Expression Grammar),
two more features are required:
- *order among ctors*, which by default is their declaration order    
- *syntactic predicates*, which are represented by special datatypes


## Special datatypes

Some special datatypes are natively recognized by Rekex as representing certain rule constructs:

- `Peek<E>` and `Not<E>` represent PEG's *and-predicate (&e)* and *not-predicate (!e)*

- `List<E>` and `E[]` represent repetition *(e&#42;)* ,
  optionally annotated with min/max sizes



## Token datatypes

At the lowest level, we need to match terminals to tokens.
This is done by *annotated types* of `String`, `enum`, etc.
that are annotated with *regular expressions*.
For example, 
`@Ch(",")char` matches a comma,
`@Regex("[0-9]+")String` matches one or more digit chars. 
Such token datatypes represent grammar rules defined in regex.

## Comparing with other approaches

### ANTLR 4

ANTLR 4 can automatically generate statically-typed datatypes and event hooks
that mirror the grammar structure.
This often requires extra labels on grammar components.
Application code is linked to grammar structure through static-typing;
however, the static-typing is derivative from grammar DSL
therefore not good for refactoring.
Both the Listener pattern and the Visitor pattern require
some amount of scaffolding code.
Most unfortunately the Visitor pattern can't declare different result 
types for different rules, making it very awkward to use.

Direct left-recursive rules are supported in ANTLR 4 as a major feature.
It is questionable though whether a left-recursive definition is really
more *natural* than a layered, flattened definition. 
Most people probably would think of `x+y+z` as a sum of `(x,y,z)`,
not as a binary tree of `((x+y)+z)`.

    ANTLR
        expr : expr '*' expr  # MulExpr
             | expr '+' expr  # AddExpr
             | num            # NumExpr 
             ;
    Rekex
        record AddExpr(SepBy1<MulExpr, Plus> children){}
        record MulExpr(SepBy1<NumExpr, Star> children){}

### Parboiled

Parboiled for Java is a conventional parser generator in the sense that
grammars are defined in a DSL;
AST constructions are done either by transforming weakly-typed parse trees,
or by imperative actions on mutable value stacks and action variables.
Parboiled for Java requires a lot of bytecode level magics which stop working 
in newer versions of Java; 
apparently there aren't enough maintenance resources to fix the problems yet.

Parboiled for Scala takes advantage of Scala's language features
to make rule definitions and AST constructions more succinct and statically-typed.
The application code is *short*, but not necessarily *simple*, 
which Scala programmers don't seem to have a problem with. 
The code appears to be functional at first glance,
but to understand the code requires understanding its imperative operation model. 


### Recursive Descent

Writing a *recursive descent parser* by hand is fun and satisfying,
at least in the beginning. You have full control and knowledge of 
what's happening at any moment of time. 
It is easy to write because you just apply the same code patterns repeatedly.
Eventually the repetition may become tedious,
and a declarative way to achieve the same goal becomes more appealing.


### Parser Combinators

In the parser combinator approach, grammar rules are mirrored by parser *functions*,
and functions can be composed from other functions. For example,
a production rule *A = B C* may be mirrored by

        Parser<A> pA = sequence(pB, pC, (b,c)->{...});

The most famous parser combinator framework is 
[Parsec](https://wiki.haskell.org/Parsec) for Haskell;
there are Java ports like
[jparsec](https://github.com/jparsec/jparsec) and
[funcj.parser](https://github.com/typemeta/funcj/tree/master/parser).

It's possible to write a parser function with arbitrary logic;
this power also means the functions are opaque,
making it generally impossible to extract a grammar to be analyzed.
Parser combinators run in a fixed execution model
which can't be replaced by an alternative parsing algorithm.
There are no representations for grammar symbols.
Functions, in most languages, do not have metadata as types have;
they don't have names or identities that can help diagnostic tasks
such as error reporting.
Surprisingly, in most Parsec ports (including FParsec for F#), 
recursive rules require *mutable* references
to construct recursive functions.



## Going Forward

Rekex introduces a new approach to parser generation
that seems superior to previously known approaches.
This new approach can be ported to a lot of other programming languages,
including older versions of Java.

Currently Rekex generates a recursive descent parser for a grammar;
nothing in the model prevents other parsing algorithms from being utilized,
which can be added to Rekex in future versions.

The author believes that the Rekex model is the future of parser generations;
he is committed to supporting it in the years to come.

----
*Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.*
