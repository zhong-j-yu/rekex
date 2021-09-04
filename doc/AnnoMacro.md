
# AnnoMacro

Java annotations are simple compile time constants, with no official mechanism
for creation and computation at runtime.

Sometimes we need a little more power from annotations. Imagine we are designing 
a library that utilizes annotations of `@Color(r,g,b)`.
For convenience of use, we also want to provide other color representations,
e.g. `@Red()`, `@HSB(h,s,b)`, that are convertible to `@Color(r,g,b)`. 
More importantly, we want the users of the library
to be able to define their own annotations, representing colors in their own ways.

[AnnoMacro](../rekex-grammar/src/main/java/org/rekex/annomacro/AnnoMacro.java) 
is a small utility for that purpose. An "annotation macro" `A1` 
contains a `public static final` field of `AnnoMacro<A1,A2>`,
which is a function that maps `A1` to `A2`.
`AnnoMacro.Resolver` resolves `A1` to `A2`, and possibly `A2` to `A3`
if `A2` is also a macro, and so on, until reaching a value that's not a macro.

    @interface Color
    {
        double r();
        double g();
        double b();
    }
    
    @interface Gray   // @Gray(v) -> @Color(r=v, g=v, b=v)
    {
        double value();
                
        AnnoMacro<Gray,Color> toColor = gray -> 
            AnnoBuilder.of(Color.class)
                .def(Color::r, gray.value())
                .def(Color::g, gray.value())
                .def(Color::b, gray.value())
                .build();
    }
    
    @interface Silver   // @Silver() -> @Gray(0.75)
    {
        AnnoMacro<Silver,Gray> toGray = silver ->
            AnnoBuilder.build(Gray.class, 0.75);
    }

    
    void test(Silver silver)  // Silver -> Gray -> Color
    {
        var resolver = new AnnoMacro.Resolver(); 
        var color = resolver.resolve(silver);
        assert color instanceof Color;
    }

---

[AnnoBuilder](../rekex-common_util/src/main/java/org/rekex/common_util/AnnoBuilder.java) 
can instantiate an annotation from name-value pairs.

    AnnoBuilder.of(Gray.class)
        .def("value", 0.75)
        .build();

    AnnoBuilder.of(Gray.class)
        .def(Gray::value, 0.75)  // method reference
        .build();

    AnnoBuilder.build(Gray.class, Gray::value, 0.75);  // @Gray(value=0.75) 

    AnnoBuilder.build(Gray.class, 0.75);  // @Gray(0.75)


You can define your own subclass for an annotation interface, 
but it's a pain to properly implement `hasCode, equals, toString` methods
as specified by `Annotation`.

`AnnoBuilder` can convert an object `a1`, which nominally implements an annotation
interface, to an object `a2`, which faithfully implements the annotation interface
with correct `hasCode, equals, toString` implementations. 
                                                      
    record GrayImpl(double value) implements Gray
    {
        public Class<Gray> annotationType(){ return Gray.class; }
    }

    Gray a1 = new GrayImpl(0.75);
    Gray a2 = AnnoBuilder.build(a1);



----
*Create by [Zhong Yu](http://zhong-j-yu.github.io).
I am looking for a job; helps appreciated.*
