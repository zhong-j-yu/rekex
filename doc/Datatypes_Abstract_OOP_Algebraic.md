# Datatypes - Abstract, OOP, Algebraic 

## Abstract datatype vs OOP

What exactly is an "abstract datatype" in an OOP language? 
Isn't everything abstract already? 

Imagine we need a data structure, say min-heap of `int`'s.
For performance reason, we decide to encode it as `int[]`.
There is a stable and final *spec* how the encoding is done.
We pass around `int[]` anywhere a heap is needed.
Anyone who needs to inspect and operate on a heap 
do it in their own code, by operating on an `int[]`, 
with conformance to the spec.

Nobody would agree that this is an "abstract" datatype.

So we survey all usages of the heap, we find that there is
a limited set of operations that people do on heaps.
So we create a set of functions, e.g. `add(int[] heap, int key)`;
we ask everybody, nicely, to use only the API to handle heaps.

If everybody does that, this data structure is pretty "abstract".
We could change the spec of the internal representation without breaking
client codes. Even if we need to change `int[]` to another type,
it is not too much a hassle to update client codes to the new type. 
Even better if we can do `typedef Heap int[]`, and everybody uses `Heap` 
as the apparent type of heaps.

There is still a bit of risk that someone reads into the `int[]`
and depends on the spec instead of the APIs. Imagine we can
establish and enforce an *abstraction boundary* such that,
inside the boundary, a `Heap` can be treated as an `int[]`; 
outside the boundary, a `Heap` can only be operated on by the API.
At this point, everybody would agree that `Heap` is an abstract datatype.
The code inside the boundary is the "provider" of the library;
the codes outside are "clients". In Java, `Heap` is usually just
a class, with private internal representation, 
and public methods as API.

What's the big deal? This is just ordinary OOP, isn't it?
An OOP practitioner rarely even thinks about abstract datatype.
But why do people distinguish between abstract datatype and OOP?

Suppose `Heap` is a `final` class; then its methods are essentially
just static functions. 
It is still an abstract datatype, but now it doesn't feel
very OOP-ish; it feels C-ish:
this `Heap` library can be implemented in C in almost the same way. 
(Sure there is "encapsulation", but encapsulation is hardly unique to OOP). 

Suppose `Heap` is an `interface`, and the library provider could
have several internal implementations. Does this make it OOP? Not so fast.
We can do that in C as well, by tagging the internal state with
the variant of the implementation, so that the library can
dispatch functions to specific implementations. This is a bit
of extra work, and it's essentially just to simulate some first
class features of an OOP language. Nevertheless, from client's point of view,
there's no difference; there is a `Heap` datatype, and there's
an API to handle it.

The real difference lies in the question - can someone else,
particularly a client, provide another implementation of Heap,
and a heap of this implementation can be passed to any consumer
that accepts `Heap` datatype? 
If the answer is yes, everybody would agree that it is OOP.
(This can be done in C with struct of functions; 
and that strategy would be considered OOP as well).
If `Heap` cannot be extended by anyone other than the library provider,
it is not so much OOP; it's not that different from an abstract datatype,
as far as clients are concerned. In Java, Heap can be `sealed`,
so that only the library provider can subclass it; the provider
can do OOP internally, in the sense that he can extend the
interface at will, and benefit from usual OOP features;
but that's irrelevant to the clients.

Operations on abstract datatypes are deterministic - a function on a given
value returns a determined value. The set of all values is fixed.
Operations on an OOP interface are less deterministic;
they of course must satisfy some general specifications,
i.e. `obj.inverse().inverse() equals obj`;
but generally we know less of the behavior of an operation
without knowing its concrete implementation.

Summary
  - If `Heap` can be extended by clients, it is definitely OOP.
    It is definitely *not* abstract datatype in the academic sense; 
  - If `Heap` cannot be extended by clients, 
    it is not very OOP, at least from client's point of view.
    It is an abstract datatype if its internal state is inaccessible to clients. 

## Algebraic datatype

An algebraic datatype is not extensible by clients.
As an example, in Java, we create a `sealed interface Option`, with
two subclasses: `record Some` and `record None`.


> Note: in Java, a sealed interface could have a subtype that's
> `non-sealed`, open to be extended, which brings OOP 
> into part of the type hierarchy. 

If `Some` and `None` are not `public`, 
invisible to clients,
clients can only operate on `Option`, 
using only methods in `Option` interface, 
then `Option` is just
an abstract datatype to clients; it's not OOP from clients point of view.
For the library provider, the exercise that `Option` is a sum of
two product types is just a choice of implementation; a rather convenient choice.
This implementation is in OOP style from the provider's point of view.

If `Some` and `None` are `public`, visible to clients, 
clients can depend on this representation of `Option`. 
Actually the provider publicizes the subtypes to
encourage clients to depend on them. This seems less abstract:
one way of representing `Option` is chosen, fixed, and exposed to the clients.
Doesn't it feel like `int[]` as `Heap` from the previous section?
Of course, now the representation is enforced by static typing, 
it's more convenient for clients; but it's still the clients that inspect
and operate on this representation on their own to achieve what they want,
assuming there are no methods in these types.
Is it abstract datatype? Not in the academic sense.
Is it OOP? Not in the usual sense; (instanceof? how could you!)
Algebraic datatype is rather quite anti-OOP; 
but that's a good news - more tools in our box.

> In Java we can have it both ways - expose `Some` and `None`,
> meanwhile have sufficient amount of methods in `Option`.
> Clients can choose which way to handle an Option,
> as an abstract datatype, or as an algebraic datatype.

> We could do algebraic types in older Java as well,
> even though without perfect compiler support.
> But Java programmers are/were extremely nervous for creating
> struct-like types, as if it's a betrayal of their creed. 
> 

Look at another example, 
[RegExp](../rekex-regexp/src/main/java/org/rekex/regexp/RegExp.java), 
which is an algebraic datatype that 
represents the structure of a regular expression as a tree.
While the library provides
some algorithm on `RegExp`, it is expected that the clients
could also create their own algorithms that require full knowledge of
the structure. `RegExp` is rather a pure specification
of a data format for communication between systems.
It is hardly OOP, even from library provider's point of view, 
since there's (almost) no methods defined in `RegExp` interface.

# Rekex

Given a Java type, Rekex derives a grammar rule for it.

From Rekex provider's point of view,
it doesn't matter what kind of type it is.
There is a list of clauses Rekex uses to map the type to a grammar rule;
one of the clause handles the case when the type is `sealed`.

From Rekex clients point of view,
they are concerned with what kind of datatypes are appropriate
to represent the parse tree and to represent the grammar.
Usually, the end result of parsing is a tree structure
that is fed to the next step, which requires full knowledge
of the structure. Algebraic datatypes are usually the better choice.
While Rekex provides a range of options of how "ctors" can be defined,
it is simplest to use the canonical constructors of `record` classes.

