# Rekex Specification 

Rekex maps every datatype to a grammar rule;
this document defines how the mapping is derived.

## Grammar Rules

Following grammar rules are supported:

- *Alternation Rule*, with one or more subrules, ordered.
- *Concatenation Rule*, with zero or more subrules, ordered.
- *Repetition Rule*, with one subrule, and `min/max` cardinality.
- *Lookahead/Lookbehind Rule*, with one subrule.
- *Regex Rule*, with a `regex`, a `flags`, a `group` number.

Each rule is also associated with
- a unique `id`
- a `datatype` of the value to be produced if the rule applies.
- an `instantiator` that produces a value if the rule applies.

The exact meaning of grammar rules are interpreted by the consumer of the rules.
The PEG parser generator interprets them according to PEG semantics.


## Datatypes, Annotated-Types 

In Rekex, datatypes are annotated-types of Java 17.
Annotations involved are all type-annotations unless specified otherwise.
Annotations on a type is a *set*, with no ordering. 
Types without annotations are also considered annotated-types 
with an empty set of annotations. 

An annotate type is one of
- a primitive type with zero or more annotations
- an array type with zero or more annotations, 
  and its component type is an annotated type.
- a class or interface type with zero or more annotations,,
  and its type arguments are annotated types.
  
Raw types and wildcards are not supported.

### Equality

Two annotated types are equal if they have the same form.
(Equality is not defined by mutual subtyping relationship.) 

### Subtyping

An annotated type `T` is a subtype of an annotated type `S`,
if 
- annotations of `T` is a super-set of annotations of `S`,

and one of the followings is true, 
- `T` and `S` are the same primitive type
- `T` and `S` are array types, and the component type of `T`
  is a subtype of the component type of `S`
- `T` and `S` are class or interface types,
  `T=Tc<argT>, S=Sc<argS>` (omitting annotations)
  - `Tc=Sc` and `argT=argS` 
  - `Tc` is a subclass of `Sc`, `Tc<params>` inherits `Sc<f(params)>`,
    and `f(argT)=argS`

For example, `@A1()LinkedList<@A2()String>` is a subtype of
`List<@A2()String>`.
  


## Datatypes to Grammar Rules

Given a target datatype, its corresponding grammar rule is derived
from following clauses, whichever succeeds first

1. For each `public ` method declared in the "ctor catalog" class,
   with 0 or more type parameters `{Ti}`, if there exists a unique
   substitution `{Ti:=Ai}` such that the method return type, with the substitution applied,
   is equal to the target datatype, the method is a "ctor" for the datatype.
   If there are one or more ctors in the catalog for the target datatype,
   invoke subprocedure *derive_from_ctor_list* with these ctors.

2. If the class/interface body of the datatype declares one or more "ctors",
   each ctor being either a public constructor, or a public static method,
   that is annotated with `@org.rekex.spec.Ctor`,
   invoke subprocedure *derive_from_ctor_list* with these ctors.

3. If the datatype is an `enum` type, it must contain one or more constant fields;
   each field must contain exactly one annotation that's convertible to
   an `@org.rekex.spec.Regex` through AnnoMacro.
   The grammar rule for the `enum` type is an *Alternation Rule*, with
   each subrule as a `Regex Rule` referencing a constant field and its `@Regex` annotation.
  
4. If the datatype class/interface is annotated with `@org.rekex.spec.Permits`,
   invoke subprocedure *derive_from_subclass_list*,
   with classes listed in the `@Permits` annotation.
  
5. If the datatype is a `sealed` type,
   invoke subprocedure *derive_from_subclass_list*, with classes in the `permits` clause.
  
6. If the datatype contains exactly one public constructor, 
   and it contains at least one constructor parameter,
   invoke subprocedure *derive_from_ctor_list* with the constructor as the only ctor.

7. If the target datatype is of `int, char, Integer, Character, String, Void` types,
   and its annotations contains exactly one annotation that's convertible to
   an `@org.rekex.spec.Regex` through AnnoMacro, return a *Regex Rule*,
   with `regex, flags, group` from the `@Regex` annotation.

8. If the target datatype is a `java.util.List<E>` or `E[]`,
   return a *Repetition Rule*, with the subrule derived from `E`,
   with `min/max` derived from intersections of all `@org.rekex.spec.SizeLimit`
   annotations converted from annotations on the target type through AnnoMacro.

9. If the target datatype is `org.rekex.spec.Peek<E>` or `org.rekex.spec.Not<E>`,
   return a *Lookahead/Lookbehind Rule*, with the subrule derived from `E`.

10. If none of the above applies, report an error.

A datatype may match more than one clauses, therefore clauses are order to resolve the conflict.
For example, an `enum` type may match (1), (2), (3), in which case (1) takes precedence.
- (1) may conflict with all others  
- (2) may conflict with (3)-(6)
- (4) may conflict with (5)-(6)
- (5) may conflict with (6)

Note that any Java type can be used as the target type in (1), e.g. `Map<String,Object>`,
even types like `int`, `String`, `List<Object>`.
This of course can be confusing and dangerous.

In summary, given a datatype, Rekex tries to

- find explicit ctors in the catalog (1)
- find explicit ctors in the class body (2)
- derive implicit ctors by (3-6)   
- token and special datatypes are handled by (7-9)
  
### subprocedures

- `derive_from_ctor_list(targetType, ctors)`
  - Order the ctors.
    A ctor is explicitly ordered if it's annotated with a `@Ctor`
    with a `value` other than `Ctor.ORDER_UNSPECIFIED`.
    If one ctor is explicitly ordered, all ctors must be explicitly ordered.
    If all ctors are explicitly ordered, they are ordered by their `@Ctor.value`.
    Otherwise, ctors are ordered by their line numbers in the source file.
  - For each ctor with 0 or more type parameters `{Ti}`,
    there must exist a unique substitution `{Ti:=Ai}` such that the result type, 
    after the substitution applied, is equal to the target type.
  - Create a *Concatenation Rule* for each ctor, with subrules derived from
    types of the constructor/method parameters, with the substitution applied. 
  - The grammar rule for the target type is an *Alternation Rule*
    with subrules corresponding to the ctors.
    
- `derive_from_subclass_list(targetType, subclasses)`
  - Order the subclasses. If they are specified by a `@Permits` annotation
    in a `Class<?>[]`, they are ordered by their array indices.
    Otherwise, subclasses are ordered by their line numbers in the source file,
    if they are defined in the same source file.
  - For each subclass `Cj<params>` with with 0 or more type parameters `{Ti}`,
    there must exist a unique substitution `{Ti:=Ai}`,
    such that the parameterized type `Cj<args>` is a subtype of the target type. 
  - For each subclass, derive a grammar rule from type ``Cj<args>``  
  - The grammar rule for the target type is an *Alternation Rule*,
    with subrules as those derived for the subclasses. 
    

## References

- [AnnoMacro](./AnnoMacro.md)

- [@Regex and equivalent](./RegexAnno.md)  

- https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.4

- https://docs.oracle.com/javase/specs/jls/se16/html/jls-4.html#jls-4.10

----
*Create by [Zhong Yu](http://zhong-j-yu.github.io)*

