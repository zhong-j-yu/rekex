package org.rekex.annotype;

import org.rekex.common_util.EscapeJavaChar;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

class PkgUtil
{
    static String simpleName(Class<?> clazz)
    {
        String name = clazz.getSimpleName();
        if(name.isEmpty()) // ??
            name = clazz.getName();
        return name;
    }
    static String canonName(Class<?> clazz)
    {
        String name = clazz.getCanonicalName();
        if(name==null) // ??
            name = clazz.getName();
        return name;
    }

    static void buildString(boolean full, Annotation anno, StringBuilder sb)
    {
        if(full)
        {
            sb.append(anno);
            return;
        }

        Class<? extends Annotation> clazz = anno.annotationType();
        sb.append('@');
        sb.append(simpleName(clazz));

        int count=0;
        Object vv = null;
        Method[] methods = clazz.getDeclaredMethods();
        Arrays.sort(methods, Comparator.comparing(Method::getName));
        for(var method : methods)
        {
            // element methods = declared public instance methods in annotation class, as of Java 16.
            if(!Modifier.isPublic(method.getModifiers()))
                continue;
            if(Modifier.isStatic(method.getModifiers()))
                continue;

            Object v;
            try
            {
                method.setAccessible(true);
                v = method.invoke(anno);  // won't return null
            }
            catch (IllegalAccessException | InvocationTargetException e) // unlikely
            {
                e.printStackTrace(); // ?
                v = e.toString(); // not fatal; toString() is for diagnosis.
            }

            // omit if same as the default value.
            if(sameValue(v, method.getDefaultValue()))
                continue;

            String name = method.getName();
            if(name.equals("value")) // handle it later
            {
                vv = v;
                continue;
            }

            ++count;
            if(count==1)
                sb.append('(');
            else
                sb.append(',');
            sb.append(name);
            sb.append('=');
            appendAnnoValue(v, sb);
        }
        if(vv!=null)
        {
            ++count;
            if(count==1) // single-element annotation
            {
                sb.append('(');
                appendAnnoValue(vv, sb);
            }
            else
            {
                sb.append(',');
                sb.append("value");
                sb.append('=');
                appendAnnoValue(vv, sb);
            }
        }
        if(count>0)
            sb.append(')');
        else
            sb.append("()"); // add parens anyway. e.g. @Foo()
    }
    static void appendAnnoValue(Object v, StringBuilder sb)
    {
        // [array of] primitive,String,Class,enum,annotation
        if(v.getClass().isArray())
            appendAnnoValueArray(v, sb);
        else if(v instanceof String str)
            EscapeJavaChar.appendQ(sb, str);
        else if(v instanceof Character ch)
            EscapeJavaChar.appendQ(sb, ch.charValue());
        else if(v instanceof Class<?> clazz)
            sb.append(simpleName(clazz)); // no ".class"
        else if(v instanceof Annotation anno)
            buildString(false, anno, sb);
        else // primitive or enum
            sb.append(v);
    }
    static void appendAnnoValueArray(Object array, StringBuilder sb)
    {
        // no {} if n==1
        int n = Array.getLength(array);
        if(n!=1)
            sb.append('{');
        for(int i=0; i<n; i++)
        {
            if(i>0)
                sb.append(',');
            Object v = Array.get(array, i);
            appendAnnoValue(v, sb);
        }
        if(n!=1)
            sb.append("}");
    }

    static boolean sameValue(Object v1, Object v2)
    {
        assert v1!=null;
        if(v2==null)
            return false;
        if(!(v1.getClass().isArray()))
            return v1.equals(v2);
        if(!(v2.getClass().isArray()))
            return false;
        int N = Array.getLength(v1);
        if(N!=Array.getLength(v2))
            return false;
        for(int i=0; i<N; i++)
            if(!Array.get(v1,i).equals(Array.get(v2,i)))
                return false;
        return true;
    }

}
