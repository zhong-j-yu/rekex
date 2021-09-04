# AST and Grammar

*A programmer's intuition on language and parsing, centered on datatypes.* 

A *language* is for conveying meanings. 
Each *sentence* in a language carries a specific meaning.

The meaning, as far as we are concerned here, is an abstract syntax tree (AST).
Each tree node has a type, and the type constrains children of the node.
It is natural to use algebraic datatypes to represent ASTs.

We don't want any ambiguities. There is a many-to-one mapping
from sentences to ASTs.
A sentence does not come from nowhere; it comes from someone
with an AST to begin with, 
who then chooses a sentence that maps to the same AST.
Sentences are just serialized forms of ASTs.

To facilitate the mapping between sentences and ASTs, 
concrete syntax trees (CSTs) are introduced in the middle,
also as algebraic datatypes.
There is a many-to-one mapping from CSTs to ASTs.
There is a one-to-one mapping between CSTs and sentences;
a CST is mapped to a sentence by simply concatenating 
its *leaf* nodes from left to right.
To make such mapping possible, 
extra syntactic nodes need to be introduced. 
How it is done is an art. 

*CST datatype is the grammar of the language.*

A parser's basic job is to map sentences to CSTs, 
using whatever techniques that reverse the mapping from CSTs to sentences.
                
Once a CST is obtained, it is easy to map it to an AST.
Some applications may equate CSTs with ASTs, 
ignoring unimportant syntactic nodes.
But, if ASTs are similar to CSTs, yet with nontrivial differences,
it is quite inconvenient to define two sets of similar datatypes;
it is a chore just to name the types.
We'd rather merge the process and have the parser output ASTs directly.

To do that, we introduce a mixed set of datatypes, *MDT*.
It contains at least the root type of AST, 
and possibly other types in AST. 
Every type *T* in MDT is mirrored to a unique *T'* in CST.
For each *T*,
we need to provide the datatype definition of *T'*, 
and, the mapping from *T'* values to *T* values.
This can be expressed succinctly, for example, by method definitions

        T t1(A a, B b){ return ... }           T' = A' B'
        T t2(C c, D d){ return ... }              | C' D'

> Note that only types in MDT are referenced by the programmer; 
  types *A*, *B*, etc. must retain enough information for the method body to construct a *T*.

Provided with these definitions, the parser can work from the root type of AST,
derive the datatypes of CST and the mapping from CST to AST.
Combined, the parser can map sentences to ASTs.
