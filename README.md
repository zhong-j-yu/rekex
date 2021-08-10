
# Rekex - grammar rules as algebraic datatypes

A context-free grammar has the form of

    A  = A1 | A2
    A1 = B C
    ...

which looks very much like definitions of algebraic datatypes.
This is by no coincidence - both formalisms reflect an underlying
model with which we build complex concepts from constituents. 

Given this correspondence, we could express a context-free grammar entirely 
as datatypes in a programming language. For example, in Java 17,

    sealed interface A permits A1, A2{}

    record A1(B b, C c) implements A{}

Such datatypes can be transliterated into a grammar, 
which is fed to a particular parser generator to build a parser.
During the parsing process, constructors of datatypes are invoked, 
eventually outputting a parse tree in the very same datatypes. 

    PegParser<A> parser = PegParser.of(A.class);

    A a = parser.matchFull(input);

**Rekex** implements this novel idea for Java 17,
addressing issues of practical concerns,
proving that it is an elegant approach to making parsers.

Read More:

- [User's Guide](doc/UsersGuide.md)
  
- [Specification](doc/Spec.md)

- [Examples](/rekex-example/src/main/java/org/rekex/exmple/parser)

- [RegExp - structured regular expression](doc/RegExp.md)
                                                  
----
<sub>Create by [Zhong Yu](http://zhong-j-yu.github.io). 
  I am looking for a Java job; helps appreciated.</sub>
