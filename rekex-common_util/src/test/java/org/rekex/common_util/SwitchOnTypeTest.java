package org.rekex.common_util;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class SwitchOnTypeTest
{

    @Test
    void testIllegalArgs()
    {
        assertThrows(IllegalArgumentException.class, ()->{
            // oddly this compiles
            var f = new SwitchOnType<Integer,Integer>()
                .on(int.class, x->2*x)
                .complete(int.class);
            // and would run just fine, if we didn't explicitly check and disallow it

            // however, even if we allowed it, the following code won't work
            // because the argument is boxed Integer
            f.apply(2);
        });

        assertThrows(IllegalArgumentException.class, ()->{
            var f = new SwitchOnType<Object,String>()
                .on(int.class, x->"int"+x)  // this compiles...
                .on(Integer.class, x->"Int"+x)
                .onDefault(String::valueOf);
            f.apply(2);
        });

        assertThrows(IllegalArgumentException.class, ()->{
            var f = new SwitchOnType<Object,String>()
                .on(Object[].class, x->"Object[]") // Object array not supported
                .onDefault(x->"default");
        });
    }

    sealed interface S{}
    sealed interface M extends S{}
    sealed interface F extends S{}
    record M1() implements M{}
    record F1() implements F{}
    record MF() implements M,F{}

    @Test
    void testIllegalOrder()
    {
        // more specific clause before more general clause
        new SwitchOnType<Object,String>()
            .on(M.class, x->"M")
            .on(S.class, x->"S")
            .onDefault(x->"default");

        var ex1 = assertThrows(IllegalStateException.class, ()->{
            var f = new SwitchOnType<Object,String>()
                .on(M.class, x->"M")
                .on(M.class, x->"M")
                .onDefault(x->"default");
        });
        //ex1.printStackTrace();

        var ex2 = assertThrows(IllegalStateException.class, ()->{
            var f = new SwitchOnType<Object,String>()
                .on(S.class, x->"S")
                .on(M.class, x->"M")
                .onDefault(x->"default");
        });
        //ex2.printStackTrace();

        var ex3 = assertThrows(IllegalStateException.class, ()->{
            var f = new SwitchOnType<Object,String>()
                .on(F.class, x->"F")
                .on(M.class, x->"M")
                .onDefault(x->"default");
        });
        //ex3.printStackTrace();

    }

    @Test
    void testPrimitiveArray()
    {
        var f = new SwitchOnType<Object,String>()
            .on(int[].class, x->"int[]")
            .onDefault(x->"default");

        assertEquals(f.apply(new int[0]), "int[]");
        assertEquals(f.apply(new long[0]), "default");
        assertEquals(f.apply(new String[0]), "default");
    }

    @Test
    void testComplete()
    {
        var f = new SwitchOnType<S,String>()
            .on(F.class, x->"F")
            .on(S.class, x->x.getClass().getSimpleName())
            .complete(S.class);
        assertEquals(f.apply(new M1()), "M1");
        assertEquals(f.apply(new MF()), "F");
        assertEquals(f.apply(new F1()), "F");
    }

    @Test
    void testCover()
    {
        var ex1 = assertThrows(IllegalStateException.class, ()->{
            var f = new SwitchOnType<S,String>()
                .on(M.class, x->"M")
                .complete(S.class); // F1 is not covered
        });
        //ex1.printStackTrace();

        {
            var f = new SwitchOnType<S,String>()
                .on(M.class, x->"M")
                .on(F1.class, x->"F1")
                .complete(S.class);
            assertEquals(f.apply(new M1()), "M");
            assertEquals(f.apply(new MF()), "M");
            assertEquals(f.apply(new F1()), "F1");
        }
        {
            var f = new SwitchOnType<S,String>()
                .on(F1.class, x->"F1")
                .on(M.class, x->"M")
                .complete(S.class);
            assertEquals(f.apply(new M1()), "M");
            assertEquals(f.apply(new MF()), "M");
            assertEquals(f.apply(new F1()), "F1");
        }

        var ex2 = assertThrows(IllegalStateException.class, ()->{
            var f = new SwitchOnType<S,String>()
                .on(M1.class, x->"M1")
                .on(MF.class, x->"MF")
                .on(M.class, x->"M") // does not cover any cases
                .complete(S.class);
        });
        //ex2.printStackTrace();


    }

    @Test
    void testFuncFunc()
    {
        var f = new SwitchOnType<S, Function<Integer,String>>()
            .on(F.class, s-> "F"::repeat)
            .on(S.class, s-> s.getClass().getSimpleName()::repeat)
            .complete(S.class);
        int i = 2;
        assertEquals(f.apply(new M1()).apply(i), "M1".repeat(i));
        assertEquals(f.apply(new MF()).apply(i), "F".repeat(i));
        assertEquals(f.apply(new F1()).apply(i), "F".repeat(i));
    }
}