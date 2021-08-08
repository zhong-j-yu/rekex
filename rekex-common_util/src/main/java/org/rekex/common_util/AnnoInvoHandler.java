package org.rekex.common_util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

class AnnoInvoHandler<A extends Annotation> implements InvocationHandler
{

    final Class<A> clazz;
    final Map<String, Object> elemValues; // containing all elements of the anno class

    AnnoInvoHandler(Class<A> clazz, Map<String, Object> elemValues)
    {
        this.clazz = clazz;
        this.elemValues = elemValues;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
    {
        String name = method.getName();
        {
            Object value = elemValues.get(name);
            if(value!=null)
                return klone.apply(value);
            // else not an element, because elem value cannot be null.
        }

        return switch (name)
        {
            case "annotationType"
                -> clazz;
            case "equals"
                -> handle_equals(args);
            case "hashCode"
                -> handle_hashCode();
            case "toString"
                -> handle_toString();
            // other methods in Object won't be dispatched to here
            default -> throw new AssertionError("unexpected method: "+method);
        };
    }

    final static Function<Object,Object> klone =
        new SwitchOnType<Object,Object>()
            .on(byte[].class, byte[]::clone)
            .on(short[].class, short[]::clone)
            .on(int[].class, int[]::clone)
            .on(long[].class, long[]::clone)
            .on(char[].class, char[]::clone)
            .on(float[].class, float[]::clone)
            .on(double[].class, double[]::clone)
            .on(boolean[].class, boolean[]::clone)
            .onDefault(v->
            {
                if( v instanceof Object[] a)
                    return a.clone();
                // otherwise v is immutable
                return v;
            });


    boolean handle_equals(Object[] args)
    {
        assert args.length==1;
        Object that = args[0];
        return that instanceof Annotation anno2
            && anno2.annotationType()==clazz
            && elemValues.entrySet().stream().allMatch(
                nv->elemEquals(that, nv.getKey(), nv.getValue()));
    }

    boolean elemEquals(Object that, String name, Object v1)
    {
        Object v2;
        try
        {
            Method method = that.getClass().getMethod(name);
            method.setAccessible(true);
            v2 = method.invoke(that);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return elmeValueEquals.apply(v1).test(v2);
    }
    final static Function<Object, Predicate<Object>> elmeValueEquals =
        new SwitchOnType<Object, Predicate<Object>>()
            .on(byte[].class, v1->v2->Arrays.equals(v1, (byte[])v2))
            .on(short[].class, v1->v2->Arrays.equals(v1, (short[])v2))
            .on(int[].class, v1->v2->Arrays.equals(v1, (int[])v2))
            .on(long[].class, v1->v2->Arrays.equals(v1, (long[])v2))
            .on(char[].class, v1->v2->Arrays.equals(v1, (char[])v2))
            .on(float[].class, v1->v2->Arrays.equals(v1, (float[])v2))
            .on(double[].class, v1->v2->Arrays.equals(v1, (double[])v2))
            .on(boolean[].class, v1->v2->Arrays.equals(v1, (boolean[])v2))
            .onDefault(v1->v2->
            {
                if( v1 instanceof Object[] a1)
                    return Arrays.equals(a1, (Object[])v2);

                assert v1!=null;
                return v1.equals(v2);
            });



    int handle_hashCode()
    {
        int hash=0;
        for(var nv : elemValues.entrySet())
        {
            String name = nv.getKey();
            Object value = nv.getValue();
            hash += (127 * name.hashCode()) ^ elemValueHashCode.apply(value);
        }
        return hash;
    }
    final static Function<Object,Integer> elemValueHashCode =
        new SwitchOnType<Object,Integer>()
            .on(byte[].class, Arrays::hashCode)
            .on(short[].class, Arrays::hashCode)
            .on(int[].class, Arrays::hashCode)
            .on(long[].class, Arrays::hashCode)
            .on(char[].class, Arrays::hashCode)
            .on(float[].class, Arrays::hashCode)
            .on(double[].class, Arrays::hashCode)
            .on(boolean[].class, Arrays::hashCode)
            .onDefault(v->
            {
                if( v instanceof Object[] a)
                    return Arrays.hashCode(a);
                assert v!=null;
                return v.hashCode();
            });


    // we require that the string is a valid piece of java code,
    // and can be compiled back to the same annotation
    String handle_toString()
    {
        StringBuilder sb = new StringBuilder();
        TreeMap<String,Object> ordered_elemValues = new TreeMap<>(elemValues);
        sb.append('@').append(canonName(clazz)).append('(');
        int count=0;
        for(var nv : ordered_elemValues.entrySet())
        {
            if(count++>0)
                sb.append(", ");
            sb.append(nv.getKey()).append('=');
            appendElemValue(sb, nv.getValue());
        }
        sb.append(')');
        return sb.toString();
    }
    void appendElemValue(StringBuilder sb, Object v)
    {
        // [array of] primitive,String,Class,enum,annotation
        if(v.getClass().isArray())
        {
            int n = Array.getLength(v);
            sb.append('{');
            for(int i=0; i<n; i++)
            {
                if(i>0)
                    sb.append(", ");
                appendElemValue(sb, Array.get(v, i));
            }
            sb.append('}');
        }
        else if(v instanceof Character ch)
        {
            EscapeJavaChar.appendQ(sb, ch);
        }
        else if(v instanceof String str)
        {
            EscapeJavaChar.appendQ(sb, str);
        }
        else if(v instanceof Class<?> c)
        {
            sb.append(canonName(c)).append(".class");
        }
        else if(v instanceof Enum<?> e)
        {
            sb.append(canonName(e.getDeclaringClass()));
            sb.append('.');
            sb.append(e.name());
        }
        else // primitive or annotation
        {
            sb.append(v);
        }
    }

    String canonName(Class<?> c)
    {
        String cn = c.getCanonicalName();
        if(cn==null) // ??
            cn = c.getName();
        return cn;
    }

}
