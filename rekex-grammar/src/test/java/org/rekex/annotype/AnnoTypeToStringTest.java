package org.rekex.annotype;

import org.junit.jupiter.api.Test;
import org.rekex.common_util.AnnoBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AnnoTypeToStringTest
{
    @interface A
    {
        int x() default 1;
        String value() default "";
    }
    String av = "abc\n\u1234";
    A a0 = AnnoBuilder.build(A.class, "");
    A a1 = AnnoBuilder.build(A.class, av);
    A a2 = AnnoBuilder.build(A.class, A::value, av, A::x, 2);

    void shortStr(AnnoType t, String s)
    {
        assertEquals(t.toString(false), s);
    }

    @Test
    void testA()
    {

        // int
        var t_int = new PrimitiveType(Set.of(), int.class);
        shortStr(t_int, "int");

        // @A0 int
        var t_a0_int = new PrimitiveType(Set.of(a0), int.class);
        shortStr(t_a0_int, "@A()int");

        // @A1[ @A0 int ]
        var t_a1_Arr_a0_int = new ArrayType(Set.of(a1), t_a0_int);
        shortStr(t_a1_Arr_a0_int, "@A()int@A(\"abc\\n\\u1234\")[]");
        // @A2[ @A1[ @A0 int ] ]
        var t_a2_arr = new ArrayType(Set.of(a2), t_a1_Arr_a0_int);
        shortStr(t_a2_arr, "@A()int@A(x=2,value=\"abc\\n\\u1234\")[]@A(\"abc\\n\\u1234\")[]");

        // @A2 Map<@A1 String, List<@A0 Integer>>
        var t_map = new ClassType(Set.of(a2), Map.class, List.of(
            new ClassType(Set.of(a1), String.class, List.of()),
            new ClassType(Set.of(), List.class, List.of(
                new ClassType(Set.of(a0), Integer.class, List.of())
            ))
        ));
        shortStr(t_map, "@A(x=2,value=\"abc\\n\\u1234\")Map<@A(\"abc\\n\\u1234\")String,List<@A()Integer>>");

    }

    enum E{ E1, E2 }
    @interface B
    {
        A[] arrA();
        Class<?>[] arrC();
        E[] arrE();
    }
    B buildB(A[] arrA, Class<?>[] arrC, E[] arrE)
    {
        return AnnoBuilder.of(B.class)
            .def(B::arrA, arrA)
            .def(B::arrC, arrC)
            .def(B::arrE, arrE)
            .build();
    }

    @Test
    void testB()
    {
        {
            B b= buildB(new A[]{}, new Class[]{}, new E[]{});
            var t = new PrimitiveType(Set.of(b), int.class);
            shortStr(t,"@B(arrA={},arrC={},arrE={})int");
        }
        {
            B b= buildB(new A[]{a0}, new Class[]{A.class}, new E[]{E.E1});
            var t = new PrimitiveType(Set.of(b), int.class);
            shortStr(t,"@B(arrA=@A(),arrC=A,arrE=E1)int");
        }
        {
            B b= buildB(new A[]{a0,a1}, new Class[]{A.class,B.class}, new E[]{E.E1,E.E2});
            var t = new PrimitiveType(Set.of(b), int.class);
            shortStr(t,"@B(arrA={@A(),@A(\"abc\\n\\u1234\")},arrC={A,B},arrE={E1,E2})int");
        }
    }

}