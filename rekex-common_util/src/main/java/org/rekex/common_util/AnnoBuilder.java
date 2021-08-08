package org.rekex.common_util;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

/**
 * Create an instance of an annotation type `A`, from name values pairs.
 * <pre>
 *     Foo foo = AnnoBuilder.of(Foo.class)
 *         .def("n1", v1)
 *         .def("n2", v2)
 *         .build();
 * </pre>
 * Method references can be used instead of names
 * <pre>
 *     Foo foo = AnnoBuilder.of(Foo.class)
 *         .def(Foo::n1, v1)
 *         .def(Foo::n2, v2)
 *         .build();
 * </pre>
 * Or use more convenient build methods
 * <pre>
 *     Foo foo = AnnoBuilder.build(Foo.class, Foo::n1, v1);
 * </pre>
 * Default values do not need to be supplied.
 */

public class AnnoBuilder<A extends Annotation>
{
    /** Return a builder for the annotation type */
    public static <A extends Annotation> AnnoBuilder<A> of(Class<A> clazz)
    {
        return new AnnoBuilder<>(clazz);
    }

    /** Single-Element Annotation, with the attribute name "value" */
    public static <A extends Annotation> A build(Class<A> clazz, Object value)
    {
        return of(clazz)
            .def("value", value)
            .build();
    }

    /** Annotation with one name-value pair */
    public static <A extends Annotation, T1> A build(Class<A> clazz,
                                                     Function<A, T1> methodRef1, T1 value1)
    {
        return of(clazz)
            .def(methodRef1, value1)
            .build();
    }
    /** Annotation with two name-value pairs */
    public static <A extends Annotation, T1> A build(Class<A> clazz,
                                                     Function<A, T1> methodRef1, T1 value1,
                                                     Function<A, T1> methodRef2, T1 value2)
    {
        return of(clazz)
            .def(methodRef1, value1)
            .def(methodRef2, value2)
            .build();
    }
    // for more name-values, chained def() looks better.

    final Class<A> clazz;
    final HashMap<String, Method> elemMethods = new HashMap<>();

    MethodTrap methodTrap;
    A trapProxy;
    HashMap<String, Object> nameValues = new HashMap<>();

    AnnoBuilder(Class<A> clazz)
    {
        this.clazz = clazz;

        for(var method : clazz.getDeclaredMethods())
        {
            // element methods = declared public instance methods in annotation class, as of Java 16.
            if(!Modifier.isPublic(method.getModifiers()))
                continue;
            if(Modifier.isStatic(method.getModifiers()))
                continue;

            elemMethods.put(method.getName(), method);
            Object defaultValue = method.getDefaultValue();
            if(defaultValue!=null)
                nameValues.put(method.getName(), defaultValue);
        }

        methodTrap = new MethodTrap();
        @SuppressWarnings("unchecked")
        A proxyA = (A)java.lang.reflect.Proxy.newProxyInstance(
            clazz.getClassLoader(), new Class<?>[]{clazz}, methodTrap
        );
        trapProxy = proxyA;
    }

    static class MethodTrap implements InvocationHandler
    {
        Method method;
        Object returnValue;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            if(this.method!=null)
                throw new IllegalArgumentException("multiple methods invoked: "+ List.of(this.method, method));
            this.method = method;

            // if the method return type is T[], and a single T value is supplied,
            // Java allows expression @A(m=value) which is equivalent to @A(m={value}).
            // T could be primitive, but never boxed primitive.
            this.returnValue = mayWrapInArray(method.getReturnType(), returnValue);

            return returnValue; // can't return null if method returns a primitive type.
            // if returnValue isn't the correct type for the method, ClassCastException will be thrown
        }
    }

    static Object mayWrapInArray(Class<?> expectedType, Object value)
    {
        if(expectedType.isArray() && !expectedType.isInstance(value))
        {
            Class<?> expectedComponentType = expectedType.componentType();
            // if value is boxed primitive type, get the primitive type
            Class<?> valueType = unboxType(value.getClass());
            if(expectedComponentType==valueType)
            {
                var array = Array.newInstance(valueType, 1);
                Array.set(array, 0, value); // it handles boxed primitive value
                return array;
            }
        }
        return value;
    }
    static Class<?> unboxType(Class<?> clazz) // e.g. Integer -> int
    {
        return MethodType.methodType(clazz).unwrap().returnType();
    }

    // T doesn't actually provides static type safety, because it could be inferred as Object.
    // It also matches def(A->X[], X), which we'll silently convert value to {value}.
    // If value isn't compatible with the return type of methodRef, an exception will be thrown.
    /** Define a name-value pair. */
    public <T> AnnoBuilder<A> def(Function<A, T> methodRef, T value)
    {
        if(value==null)
            throw new NullPointerException("annotation element value cannot be null");

        // invoke methodRef on trapProxy to figure out which method was invoked
        methodTrap.method = null;
        methodTrap.returnValue = value;
        methodRef.apply(trapProxy); // throws ClassCastException; see MethodTrap.invoke()
        var method = methodTrap.method;

        if(method==null || !elemMethods.containsKey(method.getName()))
            throw new IllegalArgumentException("methodRef did not invoke one of the element methods in "+clazz);

        // it's allowed to redefine an element with a new value.
        // this is necessary to override default values.
        // also useful for reusing a builder for multiple annos.

        String name = method.getName();
        nameValues.put(name, methodTrap.returnValue);
        return this;
    }

    /** Define a name-value pair. */
    public AnnoBuilder<A> def(String name, Object value)
    {
        Method method = elemMethods.get(name);
        if(method==null)
            throw new IllegalArgumentException("not an element name: "+name);

        // check that value is the correct type for the method
        methodTrap.method = null;
        methodTrap.returnValue = value;
        try
        {
            method.setAccessible(true);
            method.invoke(trapProxy); // throws ClassCastException; see MethodTrap.invoke()
        }
        catch (Exception e)
        {
            if(e instanceof InvocationTargetException e1)
                if(e1.getTargetException() instanceof ClassCastException e2)
                    throw e2;
            throw new RuntimeException(e);
        }

        nameValues.put(name, methodTrap.returnValue);
        return this;
    }

    /** Return an instance with previously defined name-value pairs. */
    public A build()
    {
        if(nameValues.size()<elemMethods.size())
        {
            var diff = new HashSet<>(elemMethods.keySet());
            diff.removeAll(nameValues.keySet());
            throw new IllegalStateException("value undefined for "+diff);
        }
        var elemValuesClone = new HashMap<>(nameValues);
        var handler = new AnnoInvoHandler<>(clazz, elemValuesClone);
        @SuppressWarnings("unchecked")
        A proxyA = (A)java.lang.reflect.Proxy.newProxyInstance(
            clazz.getClassLoader(), new Class<?>[]{clazz}, handler
        );
        return proxyA;
    }

    // `a0` implements an annotation interface;
    // but it may not have implemented hashCode/equals/toString() correctly.
    // (even jdk is buggy: https://bugs.openjdk.java.net/browse/JDK-8268788 )
    // this method will return an object that conforms to the spec.
    // And our toString() method returns a representation that can be compiled
    // by javac back to the same anno; JDK's does not; may not even compile.
    /**
     * Return an instance with same name-value paris as `a0`,
     * and also with correct implementations of hashCode/equals/toString
     * methods according to {@link Annotation}
     */
    public static <A extends Annotation> A build(A a0)
    {
        @SuppressWarnings("unchecked")
        Class<A> clazz = (Class<A>)a0.annotationType();
        AnnoBuilder<A> builder = new AnnoBuilder<>(clazz);
        builder.elemMethods.forEach((name, method)->{
            try
            {
                method.setAccessible(true);
                Object value = method.invoke(a0);
                builder.nameValues.put(name, value);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
        return builder.build();
    }
}
