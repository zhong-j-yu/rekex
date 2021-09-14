# Rekex, a grammar-less parser generator

By conventional wisdom, a *parser generator* needs 3 things from the user:

- *grammar rules*, defined in some rule specification language
- *AST datatypes*, defined in the target programming language
- *parser actions*, hooked to grammar rules, mostly to construct AST nodes

It is quite noticeable that, 
the structure of the grammar looks a lot like the structure of the AST datatypes, and,
many parser actions simply invoke constructors of corresponding AST datatypes.
This means there are redundancies in this model that may be eliminated.

*Rekex* is a novel parser generator that does not require explicit grammar definitions;
instead, AST datatypes, along with their (generalized) constructors,
contain all the information that's necessary for Rekex to create a parser.
This is possible because "context free" grammar rules have the same form as algebraic datatypes. 

We require that the programming language supports algebraic datatypes.
For the current release of Rekex,
Java 17, with its `sealed` and `record` types, is the target language. 


To see how this idea works, let's start with the basic case 
of generating a parser that produces concrete syntax trees.

## Concrete Syntax Tree

A *concrete syntax tree* (CST), more commonly referred to as *parse tree*,
contains all syntactic structures and data.
Leaf nodes of a CST, concatenated from left to right, 
form the exact input character sequence that is parsed into the CST. 

If our goal is to have the parser produce CSTs,
we need to define CST datatypes as dictated by the language structure.

- A sum type, `sealed interface A permits A1, A2`,
  corresponds to an alternation rule `A = A1 | A2`
- A product type, `record A1(B b, Cc)`, corresponds to a concatenation rule
  `A1 = B C`.

Given the root datatype of CST, the entire grammar can be *derived*,
and that's all we need to generate the parser.
Canonical constructors of `record` types are invoked by the parser to construct CST nodes.

> The grammar still *exists* in this model, but not in flesh.
> The user does not need to maintain a separate grammar definition file.


Things are pretty easy for CSTs. 
But what if we want to produce ASTs?

## Abstract Syntax Tree

ASTs omit unimportant syntactic information, 
and may be structurally different from CSTs.

If we have a CST produced, 
it is straightforward to transform it to an AST in a followup step.
But that is not always a pleasant coding experience. 
Most annoyingly, we may have to maintain two sets of similar datatypes.
We'd rather have the parser produce ASTs directly.

Notice that the canonical constructor of `record A1(B b, C c)` 
serves two purposes for Rekex at the same time:

1. the signature `(B, C)` expresses the grammar rule for `A1`
2. the body constructs an instance of `A1`, which stores exactly `(b, c)` 

We need to separate them if we want `A1` to be an AST node that doesn't simply store 
the syntactic children.
Suppose `A1` only stores `d` which is computed from `f(b, c)`, 
`A1` can be defined as

        record A1(D d)
        {
            @Ctor public A1(B b, C c){ this(f(b,c)); }
        }

Here, another constructor, marked by `@Ctor`, has the signature `(B, C)`,
and a body that constructs `A1(d)`. 
Rekex uses this constructor to derive the grammar rule for `A1`,
and uses it to instantiate `A1` at runtime. 
Alternatively, a static factory method marked by `@Ctor` can serve the same purpose.
In either case, Rekex refers to them as ***ctors*** for `A1`. 

*The signature of a ctor expresses the syntactic rule for the type, 
while the body constructs an instance of the type.*

Every datatype involved here mirrors a grammar rule. 
For each datatype, Rekex tries to find one or more ctors,
from which the grammar rule is derived.
The entire grammar can be derived from the root datatype of AST.

## Ctor Catalog

Ctors can be moved to a central *ctor catalog* which can contain ctors of various types.

        class CtorCatalog
        {
            public A a1(B b, C c){ return ... }
            public A a2(X x, Y y){ return ... }

            public B b(...){ return ... } 
            ...
        }

To find ctors for `A`, Rekex first searches the catalog for methods that returns `A`,
in this case, `a1` and `a2`.

> The catalog does resemble a conventional grammar definition file,
> except with only ordinary Java methods.  

## Generic Datatypes

For an optional rule `Foo?`, the user could design an algebraic datatype `OptFoo`.
But that is rather silly. Instead, Rekex supports generic datatypes like `Opt<E>`,
so that the user can simply reference `Opt<Foo>`.
Another example is `Either<A,B>`, for rule `A|B`. 
More complex forms of rules can be expressed, for example,
`Sep<T,S>` for rule `(T (S T)*)?`, that is, zero or more `T` separated by `S`.

Rekex ships with some common generic datatypes, but they are not *native* to Rekex.
Users can easily define their own generic datatypes,
for example, `record Pair<X,Y>(X x, Comma c, Y y){}`;
Rekex will perform necessary type inference and substitution for type parameters. 

## Special Datatypes

Repetition rules like `E*` can be expressed in a generic algebraic datatype;
however, since they are so common in grammars, Rekex provides native support for them,
using the familiar `List<E>` or `E[]` types. 

Rekex supports *parser expression grammar* (PEG); 
`and-predicate` and `not-predicate` in PEG are expressed in types of `Peek<E>` and `Not<E>`.
They are inserted in proper places in ctors for positive & negative lookaheads.

## Tokenization

At the lowest level, we need tokenization to match input characters.
This is expressed in datatypes of `enum`, `String`, or `char/int`(for single character),
that are annotated with `@Regex` (or equivalent annotations).

This is not a clever hack by Rekex.
Java's annotated types are designed precisely to impose more constraints on types;
an annotated type like `@Regex("[0-9]+") String` has a meaning on its own,
which limits the values that belong in the type. 



## Critiques

It's difficult for an author to remain objective, 
but I'll try to think of some potential objections to Rekex's model. 
                                              
*Explicit grammar definition is a good thing.*

It is nice to see all grammar rules in one place, in a DSL
specifically designed to present grammars in the most readable format. 
However in practice the grammar file can become really messy. 
And Rekex's ctor catalog is pretty readable too, clear and clean.

*Ctors are just rules and actions in a different representation.*

Theoretically, yes. But from engineering's point of view,
the difference is quite significant. 
In Rekex, users only need to work with ordinary Java methods, 
the simplicity, convenience, and robustness are unparallelled.  

*Rekex does not support stateful parsing.*

If you need mutable state during parsing, to influence rule matching,
or to build AST in some specific order, Rekex cannot help you.
Not only there is no built-in facility for mutable state,
you cannot even sneak one in (by thread-local variables for example), 
because there is no guarantee on ordering between invocations of ctors 
(except, of course, ctors of child nodes are invoked before the ctor of a parent node).


## Comparison with Parboiled

Parboiled is an excellent PEG parser generator.
It is still a conventional parser generator in the sense that
it also requires explicit grammar definition and parser actions, 
even though they are all done in Java code (with some magics),
which probably accounts for its popularity.
To construct ASTs, users need to work with value stacks and parser actions,
which can end up very messy.

In Parboiled, grammar rules are built as objects, which raises an interesting
question of how to create recursive rules which requires cyclic object graphs.
In Rekex, grammar rules corresponds to types, and recursive types have never been a problem. 

Judging from Parboiled's issue page, it starts to break down in newer versions of Java,
and the maintenance effort cannot keep up. 
Rekex wishes to be the successor of Parboiled; 
the author is committed to support and improve it in the years to come, 
with a strong belief that it is the better solution 
that can benefit fellow developers greatly.  


----
*Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.*
