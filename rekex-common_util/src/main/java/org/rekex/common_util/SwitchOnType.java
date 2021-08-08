package org.rekex.common_util;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;

// simulate the future Java feature of Pattern Matching for switch.
//     https://openjdk.java.net/jeps/406
// mainly for sealed class trees, to make sure there's no uncovered cases.
// could also be used, with a default clause, to search through
// a big set of final classes, as opposed to chained if-instanceof.

/**
 * Build a function that maps `T` to `R`, from functions that maps subtypes of `T` to `R`.
 * Either a default clause must be provided, e.g.
 * <pre>
 *     var func = new SwitchOnType&lt;Number, String&gt;()
 *         .on(Integer.class, i->"int")
 *         .on(Long.class, l->"long")
 *         .onDefault(n->"num");
 * </pre>
 * Or it must be asserted that all subtypes are covered, e.g.
 * <pre>
 *     var func = new SwitchOnType&lt;Foo, String&gt;()
 *         .on(Foo1.class, f1->"f1")
 *         .on(Foo2.class, f2->"f2")
 *         .complete(Foo.class);
 * </pre>
 */
public class SwitchOnType<T, R>
{
    HashMap<Class<?>, Class<?>> clazzToCover = new HashMap<>();
    HashMap<Class<?>, Function<?,R>> clazzToFunc = new HashMap<>();

    public SwitchOnType()
    {
    }

    // oddly, primitive classes could pass through type inferences;
    // but ultimately Function only works on Boxed types.
    static void disallowPrimitiveClass(Class<?> clazz)
    {
        if(clazz.isPrimitive())
            throw new IllegalArgumentException("primitive class not allowed: "+clazz);
    }

    public <S extends T> SwitchOnType<T,R> on(Class<S> clazz, Function<S, R> func)
    {
        disallowPrimitiveClass(clazz);

        // we depend on class identity to map obj to func.
        // therefore we need to know all final subclasses covered by `clazz`.
        HashSet<Class<?>> subs = new HashSet<>();
        findAllFinalSubclasses(clazz, subs); // throws

        int covered = 0;
        for(var sub : subs)
        {
            Class<?> c1 = clazzToCover.get(sub);
            if(c1==null) // no previous clause covering `sub`
            {
                clazzToCover.put(sub, clazz);
                clazzToFunc.put(sub, func);
                covered++;
            }
            else
            {
                final var c2 = clazz;
                // a previous on(c1, f) clause had covered this sub class.
                // the more specific one of {c1, c2} takes precedence.
                // to avoid any confusion, we requires that,
                // the more specific one must be defined first in program order,
                // as if it's defined first in a chain of if-instanceof statements.
                boolean s1 = c2.isAssignableFrom(c1);
                boolean s2 = c1.isAssignableFrom(c2);
                if(s1 && !s2) // c1 is more specific; good.
                    continue; // previous clause is not overridden.
                if(!s1 && !s2) // no relationship
                    throw new IllegalStateException(
                        "conflicting clauses covering %s: [%s; %s]".formatted(sub, c1, c2));
                if(s1 && s2) // c2=c1
                    throw new IllegalStateException(
                        "duplicate clauses for: %s".formatted(c1));
                // c2 is more specific
                throw new IllegalStateException(
                    "illegal order of clauses: [%s; %s]".formatted(c1, c2));
            }
        }
        if(covered==0) // unreachable code; raise error per Java's convention.
            throw new IllegalStateException("no case is covered by: "+clazz);
        return this;
    }

    static void findAllFinalSubclasses(Class<?> clazz, HashSet<Class<?>> set)
    {
        if(Modifier.isFinal(clazz.getModifiers()))
        {
            // primitive arrays are fine; supported; useful;
            // Object arrays are more complicated; not supported for now.
            if(clazz.isArray() && !clazz.getComponentType().isPrimitive())
                throw new IllegalArgumentException("Object array not supported: "+clazz);

            set.add(clazz);
        }
        else
        {
            var subs = clazz.getPermittedSubclasses();
            if(subs==null) // clazz is open for extension
                throw new IllegalArgumentException("unsupported open class: "+clazz);
            for(var sub : subs)
                findAllFinalSubclasses(sub, set);
        }
    }

    public Function<T, R> onDefault(Function<T, R> func)
    {
        var clazzToFuncCopy = new HashMap<>(clazzToFunc);
        return new Func<>(clazzToFuncCopy, func);
    }

    public Function<T, R> complete(Class<T> rootClass)
    {
        disallowPrimitiveClass(rootClass);
        // required: rootClass is a closed type; all final subclasses are covered;
        HashSet<Class<?>> subs = new HashSet<>();
        findAllFinalSubclasses(rootClass, subs); // throws
        subs.removeAll(clazzToFunc.keySet());
        if(!subs.isEmpty())
            throw new IllegalStateException("uncovered cases: "+subs);

        Function<T, R> defaultFunc = input -> {
            if(input==null)
                throw new NullPointerException("null input is not supported");
            throw new AssertionError("uncovered class: "+input.getClass());
        };
        var clazzToFuncCopy = new HashMap<>(clazzToFunc);
        return new Func<>(clazzToFuncCopy, defaultFunc);
    }


    static class Func<T, R> implements Function<T, R>
    {
        final Map<Class<?>, Function<?,R>> clazzToFunc;
        final Function<T, R> defaultFunc;

        public Func(Map<Class<?>, Function<?, R>> clazzToFunc, Function<T, R> defaultFunc)
        {
            this.clazzToFunc = clazzToFunc;
            this.defaultFunc = defaultFunc;
        }

        @Override
        public R apply(T input)
        {
            if(input==null)
                return defaultFunc.apply(null); // which may or may not accept null

            Class<?> clazz = input.getClass();
            Function<?, R> func = clazzToFunc.get(clazz);
            if(func==null)
                func = defaultFunc;
            @SuppressWarnings("unchecked")
            Function<T, R> func2 = (Function<T, R>)func;

            return func2.apply(input);
        }
    }

}
