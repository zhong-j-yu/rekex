package org.rekex.annotype;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all") // suppress "never used"
class TypeMathInferTest
{
    @Test
    void test()
    {
        var clazz = this.getClass();
        for(var c : clazz.getNestMembers())
        {
            String cn = c.getSimpleName();
            if(cn.startsWith("X"))
            {
                //System.out.println(cn);
                test(c);
            }
        }
    }

    static AnnoType typeOf(Class<?> clazz, String fieldName) throws Exception
    {
        var t = clazz.getDeclaredField(fieldName).getAnnotatedType();
        return TypeMath.convertFromJlr(t);
    }
    void test(Class<?> clazz)
    {
        AnnoType expect = null;
        AnnoType inferred;
        try
        {
            var target = typeOf(clazz, "target");
            var source = typeOf(clazz, "source");
            expect = typeOf(clazz, "expect");
            var varMap = TypeMath.inferredSubtype(source, target);
            if(varMap==null)
                inferred = ClassType.withTypeVars(Void.class);
            else
                inferred = TypeMath.doTypeVarSubstitution(source, varMap);
            if(expect.equals(inferred))
                return;
        }
        catch (Exception e)
        {
            throw new RuntimeException("error in "+clazz, e);
        }

        throw new AssertionFailedError("inference fail: "+clazz, expect, inferred);
    }

    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface A{ int value() default 0; }
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface A2{ int value() default 0; }

    class X42352345
    {
        int target;
        int source;
        int expect;
    }
    class X341234123
    {
        long target;
        int source;
        Void expect; // do not consider int as subtype of long
    }
    class X32451
    {
        @A int target;
        int source;
        Void expect; // anno doesn't match
    }
    class X4545
    {
        int target;
        @A int source;
        @A int expect;
    }
    class X35625
    {
        @A int target;
        @A int source;
        @A int expect;
    }
    class X4545543
    {
        int[] target;
        int[] source;
        int[] expect;
    }
    class X45452345
    {
        int[] target;
        @A int[] source;
        @A int[] expect;
    }
    class X347456
    {
        Number[] target;
        Integer[] source;
        Integer[] expect;
    }
    class X4523412
    {
        Number[] target;
        Integer @A[] source;
        Integer @A[] expect;
    }
    class X08234124
    {
        Number @A[] target;
        Integer @A[] source;
        Integer @A[] expect;
    }
    class X6353456
    {
        Number[] target;
        @A Integer[] source;
        @A Integer[] expect;
    }
    class X346456
    {
        Number[] target;
        @A Integer @A[] source;
        @A Integer @A[] expect;
    }
    class X452435
    {
        Number @A[] target;
        Integer[] source;
        Void expect;   // [] is not @A[]
    }

    class X423152345
    {
        Number target;
        String source;
        Void expect;     // not subclass
    }
    static class C0{}
    static class C1 extends @A(1)C0{} // annotated supertype
    class X52345
    {
        C0 target;
        C1 source;
        C1 expect;
    }
    class X3241234
    {
        @A(1)C0 target;
        C1 source;
        C1 expect;
    }
    class X0988
    {
        @A(2)C0 target;
        C1 source;
        Void expect;  // C1 is not @A(2)C0
    }

    class X198011<T> // overwhelmingly, just simple type var inference
    {
        List<String> target;
        ArrayList<T> source;
        ArrayList<String> expect;
    }
    class X435234<T>
    {
        List<@A String> target;
        List<T> source;
        List<@A String> expect; // T-> @A String
    }
    class X567356<T>
    {
        List<@A String> target;
        List<@A T> source;
        List<@A String> expect; // T-> String
    }
    class X657346<T>
    {
        List<@A@A2 String> target;
        List<@A T> source;
        List<@A@A2 String> expect; // T-> @A2 String
    }
    class X32452352<T>
    {
        List<String> target;
        List<@A T> source;
        Void expect; // @A T = String fail
    }
    class X32452345<T>
    {
        List<@A2 String> target;
        List<@A T> source;
        Void expect; // @A T = @A2 String  fail
    }
    class X473456<T extends Number>
    {
        List<String> target;
        List<T> source;
        Void expect;  // T=String, not subtype of Number
    }
    class X45234134<T extends @A String>
    {
        List<String> target;
        List<T> source;
        Void expect;  // T=String, not subtype of @A String
    }
    class X56245<T extends @A String>
    {
        List<@A@A2 String> target;
        List<T> source;
        List<@A@A2 String> expect;  // T is subtype of @A String
    }
    class X562456
    {
        @A(1)String target;
        @A(2)String source;
        Void expect;
    }
    class X24562345
    {
        List<@A(1)String> target;
        List<@A(2)String> source;
        Void expect;
    }
    class X5235235423<T>
    {
        String target;
        T source;
        String expect;
    }
    class X5463456<T>
    {
        int target;
        T source;
        Void expect; // T=int, does not satisfies the bound Object
    }
    class X24523452345<T>
    {
        @A String target;
        T source;
        @A String expect;
    }
    class X4512341234<T>
    {
        String[] target;
        T[] source;
        String[] expect;
    }
    class X34123<T>
    {
        @A String[] target;
        T[] source;
        @A String[] expect;
    }
    class X546234<T>
    {
        Map<String,String> target;
        Map<T,T> source;
        Map<String,String> expect;
    }
    class X41234124<T>
    {
        Map<String,Number> target;
        Map<T,T> source;
        Void expect; // conflict T=String,T=Number
    }
    class X26245<T>
    {
        Map<@A(1)String,@A(2)String> target;
        Map<T,T> source;
        Void expect; // conflict
    }
    class X412421<T>
    {
        List<int[]> target;
        List<T> source;
        List<int[]> expect;
    }
    class X353456<T>
    {
        List<int[]> target;
        List<T[]> source;
        Void expect;  // T=int, not subtype of Object
    }
    class X25625462<T>
    {
        List<@A String[]> target;
        List<T[]> source;
        List<@A String[]> expect;
    }
    class X43334232<T>
    {
        List<@A String @A[]> target;
        List<T @A[]> source;
        List<@A String @A[]> expect;
    }
    class X4757<T>
    {
        List<String @A[]> target;
        List<T[]> source;
        Void expect;  // [] is not @A[]
    }

}