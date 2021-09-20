# Rekex



The basic idea is this: 
For every non-terminal symbol *A* in the grammar, define a datatype `A`.
For every production rule *A = B C* in the grammar,
define a method that returns `A` based on `(B,C)`

        public A a(B b, C c){ return ...; }

There is no seperate grammar file; 
the collection of such datatypes and methods precisely defines the grammar,
and that's everything needed to generate a parser,
which invokes the methods to create AST nodes.

It can get even simpler:
if datatype `A` is designed to be a record of `(B, C)`,
the production rule *A = B C* can be implied by the datatype definition

        record A(B b, C c){}

This is the minimum amount of code that must be written anyway,
assuming we want *statically-typed* trees out of the parser.
Therefore this is objectively the simplest possible approach to parser generation,
and, arguably the most intuitive approach.

Let's first take a look at the old ways and see why they are more complicated.

## Conventional parser generators

Conventional parser generators, like ANTLRv4 and Parboiled,
start from a grammar definition file,
along with parser actions that are hooked to grammar rules in various places.
The end goal of parser actions is to build an AST as the result of a parsing process.
For "maximum power and flexibility", parser actions are allowed do *anything*. 
Since the parser generator cannot participate in how ASTs are constructed, 
parser actions must maintain and manage *mutable states* among themselves.
This means they must be invoked in a very specific order, 
as defined by the parser generator. 

Imperative programming in general isn't that bad 
when it's written as a continuous sequence of statements.
But here, imperative statements, along with mutable variable declarations, 
are scattered around as code snippets,
in an order that's not immediately obvious.
To piece them together and figure out what's happening requires some brain power.

weakly typed. convoluted to build an AST tree.
grammar driven, can't work with user defined types because they do not know user defined types, 

## A simpler view on parsing

Most programmers probably have a simpler, more intuitive view on parsing --

A non-terminal symbol in a grammar isn't merely an identifier,
it carries a meaning.
If a phrase matches a production rule for a symbol,
we'd like to produce a data node to represent the phrase,
in a datatype that represents the meaning of the symbol;
and the data node should be constructed from child nodes representing subphrases.



In this simpler view,

- Every non-terminal symbol *A* is mirrored by a datatype `A`
- If a phrase matches a production rule *L = R1 ... Rn*,
  - a data node of datatype `L` should be constructed to represent the phrase
  - it is constructed from child nodes of types `R1 ... Rn`,
    which are constructed for subphrases.

This is a restricted model on how AST should be constructed,
but it's probably a very common model for many use cases.

One major limitation of this model is that rule matching 
is strictly context free, it cannot depend on preceding phrases;
whereas in a conventional parser generator,
effects from earlier actions can influence current rule matching
through semantic predicates.
In our simpler model, we can also add semantic predicate for a production rule,
but it can only veto the rule with information from subphrases.

Another limitation is error tolerance. 

For programmers that do think in this model,
conventional parser generators aren't very user friendly;
too much scaffolding code must be written to achieve the obvious.
This is where Rekex comes in.




## Rekex

In Rekex, a grammar is expressed as a collection of Java datatypes and methods,
in a straightforward way such that it's easy to translate 
between the two in either directions.
          
Non-terminal symbols in the grammar are represented by datatypes in Java, 
in one-to-one correspondence.
There are 3 categories of datatypes:

- *token datatypes*, for matching terminals with regular expressions
- *special datatypes*, for special rule constructs
- *composite dataypes*, for symbols with one or more production rules.
  They can be any Java type, including primitive types.

For every composite datatype, there's one or more ctors. 
A ***ctor*** is a method that represents a production rule, 
along with the action that instantiate an AST node when the rule matches.
For example,

        public A a(B b, C c) throws E { ... }

- the return type and parameter types expresses a production rule *A = B C*
- the throws clause indicates possible types of semantic predicate failures
- the body either returns an instance of `A`, or raises one of the failures
- the body can only depend on arguments (b,c), 
  and possibly some config data that remain constant in a parsing process
- method name and parameter names are insignificant to Rekex  

There is no guarantee in which order ctors are invoked during a parsing process,
or even the number of times a ctor is invoked.
Therefore it is impossible to do imperative programming among ctors;
ctors should be *pure functions*.
                                                   
For any composite datatype, Rekex finds its ctors either in a central *ctor catalog* class, 
or within the class/interface that defines the datatype.
Given the *root datatype* that represents the *start symbol* of a grammar,
Rekex recursively discovers all datatypes and ctors for the entire grammar.
A parser is then generated for this grammar that outputs ASTs in the root datatype.

It is fair to say that there's unprecedented simplicity, clarity, 
and elegance in this novel approach to parsing.
It is ***trivial*** to create a parser this way.
But wait, there's more.


## Implicit ctors

Often there are grammar rules like *A = A1 | A2* ,
and it is natural to design datatype `A` as the parent type of `A1, A2`.
In Java 17, it may look like `sealed interface A permits A1, A2{}` .
The grammar rule *A = A1 | A2*, which is really sum of two production rules, *A=A1, A=A2*, 
can be expressed in ctors like

        public A a1(A1 a1){ return a1; }
        public A a2(A2 a2){ return a2; }

These ctors can be omitted. If there's no explicit ctors for a datatype `A`,
and `A` is a *sealed* type with subtypes `A1, A2`,
ctors like these are implicit declared for `A`.

Next, suppose *A1* has one production rule *A1 = X Y*, 
and datatype `A1` has the same form as `record A1(X x, Y y){}`,
it feels rather redundant to have to declare the ctor as

        public A1 a1(X x, Y y){ return new A1(x, y); }        

This ctor can be omitted as well. If there's no explicit ctors for a datatype `A1`,
and `A1` has only one public constructor, 
the constructor is the implicit ctor for `A1`.

In summary, variant rules could be expressed in `sealed` types,
and production rules in `record` types, without the need of explicit ctors.
Rekex requires Java 17 to utilize such types for grammars. 

## Grammar as algebraic datatypes

With implicit ctors, a context free grammar can be
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
while other works are done in post-processing,
which is easy because the parse tree is statically-typed 
in user defined algebraic datatypes.

If we do want some kind of "abstract" syntax trees out of the parser,
we need to refactor some datatypes,
either to discard some syntactic information, 
or to store information in different structures.
For example, we may want our AST nodes that represent arithmetic expressions
to store nothing but the computed numeric value.
The refactoring from concrete to abstract can be done incrementally.
If we started with `record A1(X,Y)`, and now we want it to become `record A1(Z)`,
we can first introduce an
explicit ctor to keep representing the grammar rule for *A1*

        public A1 a1(X x, Y y){ return new A1(x, y); }

then we can safely refactor `record A1` without breaking grammar rules.
A *ctor*, therefore, is a generalized constructor for a datatype
that requires all syntactic components, 
regardless of how they are retained in the datatype.
              
This engineering approach, working from parse tree to AST, has the advantage
that we can quickly and correctly model the grammar as a first step,
getting a useful parser with very little effort;
then we can improve on it progressively, 
confident that correctness is maintained throughout refactoring processes.
It's easy to get started, and pleasant to move forward.


## Generic datatypes

Type parameters in datatypes and ctors are supported by Rekex,
which performs type inference and substitutions whenever necessary.
This mechanism enables us to design generic datatypes for generic rule constructs,
for example, `Opt<E>` for *E?* , `Either<A,B>` for *(A | B)* , 
and `SepBy<T,S>` for *(T (S T)\*)?* . 
Rekex ships with some commonly used generic datatypes;
users can easily define their own.
They are crucial for expressing grammar rules succinctly
with in-line subrules; for example

        public Integer integer(Opt<Sign> sign, OneOrMore<Digit> digits){ ... }

## PEG support

The general ideas explored so far can be applied to any grammar
that is mostly "context free" in form.
Currently, Rekex specifically supports Parser Expression Grammar (PEG),
which differs from Context-free Grammar in two ways:

- In PEG, order is significant among productions rules with the same l.h.s. symbol. 
  Rekex by default orders ctors in their textual order in the source file.  

- PEG has syntactic predicates.
  Rekex uses special datatypes to represent them.


## Special datatypes

Some special datatypes are natively recognized by Rekex as representing certain rule constructs.

- `Peek<E>` and `Not<E>` represent PEG's *and-predicate (&e)* and *not-predicate (!e)*

- `List<E>` and `E[]` represent repetition rules *(e&#42;)* ,
  optionally annotated with min/max sizes
  
- `ParseInfo` retains information of input regions that matched a ctor and its arguments.
  

## Token datatypes

At the lowest level, we need to match terminals to tokens.
This is done by *annotated types* of `String`, `enum`, etc.
that are annotated with regular expressions.

Java's annotated types are designed precisely to impose more constraints on types.
When we need a datatype for non-empty strings that contain only digit chars, 
we can express it as `@Regex("[0-9]+")String`. 
This datatype corresponds to a grammar rule that matches terminals with the regular expression.

Referencing such datatypes directly can be distracting because of the verbosity.
They can be extracted to keep the rest of the grammar clean. For example 

    // tokens
    record Digits( @Regex("[0-9]+")String s ){}
    ...
    // rules
    record Assign(Name n, Eq e, Digits v){}
    

## Comparing with Recursive Descent

## Comparing with Parser Combinators


## Comparing with ANTLRv4

direct left-recursion

    expr : expr '*' expr  # MulExpr
         | expr '+' expr  # AddExpr
         | atom           # AtomExpr 
         ;


    record AddExpr(SepBy1<MulExpr,  Plus> subs){}
    record MulExpr(SepBy1<AtomExpr, Star> subs){}

Listeners. Visitors.
parser generated types.  subtyping.
brittle to refactoring.
scalfolding.
visitors can only return one type, not good for heterogeneous AST node types.

## Comparing with Parboiled
                  
The closest competitor to Rekex is Parboiled,
an established PEG parser generator for Java community.
Parboiled constructs grammar rules with Java APIs,
which probably accounts for its popularity.
However, it is still a conventional parser generator
with imperative parser actions, requiring complicated 
interaction with value stacks and action variables to construct ASTs.
Some of the apparent simplicity are achieved by magics
which nonetheless complicates understanding and using the framework.   

One particularly interesting point is that, in Parboiled, 
grammar rules are constructed as rule objects referencing other rule objects, 
raising the question of how to create recursive rules, which requires cyclic object references.
Parboiled solved that problem with a clever magic.
While in Rekex, grammar rules are represented by types referencing types,
and recursive types have never been a problem in Java.

Judging from Parboiled's Issues page, 
the magics start to malfunction in newer versions of Java,
and maintenance resources cannot keep up with rising problems. 
Rekex is poised to be the successor of Parboiled for the future;
the author is committed to support and improve it in the years to come,
with a strong belief that it is the better solution
that can greatly benefit the community.


----
*Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.*
