package org.rekex.annomacro;

import org.junit.jupiter.api.Test;
import org.rekex.common_util.AnnoBuilder;

import java.lang.annotation.*;

import static org.junit.jupiter.api.Assertions.*;

class AnnoBuilderTest
{
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface AnnoX
    {
        String value();

        int flags() default 0;

        int group() default 0;
    }

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface AnnoY
    {
        String[] strings();
        int[] ints();
        boolean[] bools() default{};
    }


    @AnnoX(value = "ab\ncd", flags = 1, group = 2)
    @AnnoY(strings={"A","B"}, ints={1})
    static class ClassX{}

    AnnoX ax = ClassX.class.getAnnotation(AnnoX.class);

    @Test
    void testArrays()
    {
        AnnoY a1 = ClassX.class.getAnnotation(AnnoY.class);

        AnnoY a2 = AnnoBuilder.of(AnnoY.class)
            .def(AnnoY::strings, a1.strings())
            .def(AnnoY::ints, a1.ints())
            .build();
        assertArrayEquals(a2.strings(), a1.strings());
        assertArrayEquals(a2.ints(), a1.ints());
        assertEquals(a2.hashCode(), a1.hashCode());
        assertEquals(a1, a2);
        assertEquals(a2, a1);

        assertEquals(a2.toString(),
            "@org.rekex.annomacro.AnnoBuilderTest.AnnoY(bools={}, ints={1}, strings={\"A\", \"B\"})");
    }

    @Test
    void testArrayOfOne()
    {
        // when an element type is T[], it is ok to supply a single T value
        AnnoY a2 = AnnoBuilder.of(AnnoY.class)
            .def(AnnoY::strings, "a")
            .def(AnnoY::ints, 1)
            .def(AnnoY::bools, false)
            .build();
        assertEquals(a2.toString(),
            "@org.rekex.annomacro.AnnoBuilderTest.AnnoY(bools={false}, ints={1}, strings={\"a\"})");
    }

    @Test
    void testSingleElementAnno()
    {
        var a1 = AnnoBuilder.build(AnnoX.class, "abc");
        assertEquals(a1.toString(),
            "@org.rekex.annomacro.AnnoBuilderTest.AnnoX(flags=0, group=0, value=\"abc\")");
    }

    private void checkAx(AnnoX r1)
    {
        assertEquals(r1.annotationType(), AnnoX.class);

        assertEquals(ax.hashCode(), r1.hashCode());
        assertEquals(ax, r1);
        assertEquals(r1, ax);

        String str = r1.toString();
        assertEquals(str, "@org.rekex.annomacro.AnnoBuilderTest.AnnoX(flags=1, group=2, value=\"ab\\ncd\")");
    }

    @Test
    void testFoo1()
    {
        AnnoX r1 = AnnoBuilder.of(AnnoX.class)
            .def(AnnoX::value, ax.value())
            .def(AnnoX::flags, ax.flags())
            .def(AnnoX::group, ax.group())
            .build();
        checkAx(r1);
    }

    @Test
    void testFoo2()
    {
        AnnoX r1 = AnnoBuilder.of(AnnoX.class)
            .def("value", ax.value())
            .def("flags", ax.flags())
            .def("group", ax.group())
            .build();
        checkAx(r1);
    }
    @Test void testFoo3()
    {
        AnnoX x = new AnnoX(){
            public String value(){ return ax.value(); }
            public int flags() { return ax.flags(); }
            public int group() { return ax.group(); }
            public Class<AnnoX> annotationType() { return AnnoX.class; }
        };
        AnnoX r1 = AnnoBuilder.build(x);
        checkAx(r1);
    }
    @Test void testFoo4()
    {
        record Impl(String value, int flags, int group) implements AnnoX
        {
            @Override
            public Class<? extends Annotation> annotationType()
            {
                return AnnoX.class;
            }
        }

        Impl x = new Impl(ax.value(), ax.flags(), ax.group());
        //Impl r1 = AnnoBuilder.build(x); // compiles, but fail at runtime
        //var  r1 = AnnoBuilder.build(x); // same failure; var=Impl
        AnnoX r1 = AnnoBuilder.build(x);
        checkAx(r1);
    }



    @Test
    void testWrongValueTypes()
    {
        Exception ex=null;

        ex = assertThrows(ClassCastException.class, ()->{
            AnnoBuilder.of(AnnoX.class).def(AnnoX::flags, "string");
        });

        ex = assertThrows(ClassCastException.class, ()->{
            AnnoBuilder.of(AnnoX.class).def("flags", "string");
        });

        // no automatic widening like short->int
        ex = assertThrows(ClassCastException.class, ()->{
            AnnoBuilder.of(AnnoX.class).def("flags", Short.valueOf((short)1));
        });
        ex = assertThrows(ClassCastException.class, ()->{
            AnnoBuilder.of(AnnoY.class).def("ints", Short.valueOf((short)1));
        });

    }


    // to string of Class and Enum
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @AnnoZ(classes = {int.class, int[].class, String.class, AnnoZ[].class}, enums=ElementType.TYPE_USE)
    @interface AnnoZ
    {
        Class<?>[] classes();
        ElementType[] enums();
    }
    @Test
    void testToString()
    {
        AnnoZ az = AnnoZ.class.getAnnotation(AnnoZ.class);
        // System.out.println(az.toString());
        // @org.rekex.annomacro.AnnoBuilderTest$AnnoZ(classes={int.class, int[].class, java.lang.String.class, rekex.annomacro.AnnoBuilderTest$AnnoZ[].class}, enums={TYPE_USE})

        AnnoZ az2 = AnnoBuilder.of(AnnoZ.class)
            .def(AnnoZ::classes, az.classes())
            .def(AnnoZ::enums, az.enums())
            .build();
        assertEquals(az2.toString(), "@org.rekex.annomacro.AnnoBuilderTest.AnnoZ(classes={int.class, int[].class, java.lang.String.class, org.rekex.annomacro.AnnoBuilderTest.AnnoZ[].class}, enums={java.lang.annotation.ElementType.TYPE_USE})");

        assertEquals(az, az2);
        assertEquals(az.hashCode(), az2.hashCode());
    }

    @Test
    void testToString2()
    {
        // marker interface, no element
        FunctionalInterface a0 = AnnoBuilder.of(FunctionalInterface.class).build();
        assertEquals(a0.toString(), "@java.lang.FunctionalInterface()");
        // one element
        Retention a1 = AnnoBuilder.build(Retention.class, RetentionPolicy.RUNTIME);
        assertEquals(a1.toString(), "@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)");
    }

}