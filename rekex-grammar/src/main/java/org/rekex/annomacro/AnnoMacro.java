package org.rekex.annomacro;

import org.rekex.annotype.ClassType;
import org.rekex.annotype.TypeMath;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;

/**
 * For creating annotation macros.
 * <p>
 *     We call an annotation interface `A1` a macro,
 *     if it contains a constant field of `AnnoMacro&lt;A1,A2&gt;`,
 *     which is just a function mapping A1 to A2.
 * </p>
 * <p>
 *     {@link Resolver} uses these functions to map annotations to annotations.
 * </p>
 */
public interface AnnoMacro<T, R>
{
    /** map input to output */
    R expand(T t);

    // actually, we don't enforce that T,R are Annotations;
    // intermediary non-Annotation types could be used in the chain of expansions.
    // nevertheless, this util is for Annotation->...->Annotation transformations.

    /**
     * To resolve annotation macros.
     * <p>
     *     An instance of Resolver is thread safe.
     *     It should be shared for repeated use, because it maintains an internal cache.
     * </p>
     */
    class Resolver // user creates an instance for a session of works. it has internal cache.
    {
        /**
         * If `v` is a macro, invoke the mapping function on it;
         * do this recursively, until a value is reached that's not a macro.
         */
        public <R> R resolve(Object v)
        {
            while(true)
            {
                AnnoMacro<Object,?> f = getConverter(v.getClass());
                var v2 = f.expand(v);
                if(v2==v) // no more expansions
                {
                    @SuppressWarnings("unchecked")
                    var r = (R)v;
                    return r;
                }
                else
                {
                    v = v2;
                    // continue to resolve `v`
                }
            }
        }

        // gc `this` to gc the cache. no static-scope caching.
        ConcurrentHashMap<Class<?>, AnnoMacro<Object, ?>> converterCache = new ConcurrentHashMap<>();

        AnnoMacro<Object,?> getConverter(Class<?> clazz)
        {
            var f = converterCache.get(clazz);
            if(f==null)
            {
                try
                {
                    f=findConverter(clazz);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("error in finding a converter for "+clazz, e);
                }
                converterCache.put(clazz, f);
            }
            return f;
        }

        AnnoMacro<Object,?> findConverter(Class<?> clazz) throws Exception
        {
            Field field = null;
            // there should be *one* public AnnoMacro field in the class.
            // it's possibly inherited from a supertype.
            for(var f : clazz.getFields())
            {
                if(!AnnoMacro.class.isAssignableFrom(f.getType()))
                    continue;
                if(field!=null)
                    throw new Exception("multiple public fields of subtype of "+AnnoMacro.class);
                field = f;
                // continue to search if there's more
            }

            if(field==null) // all right, no more macro expansions.
                return x->x;

            // there is one public AnnoMacro field declared;
            // it must conform to our usage standards.

            // must be static final
            if(!Modifier.isStatic(field.getModifiers()))
                throw new Exception("should be static: "+field);
            if(!Modifier.isFinal(field.getModifiers()))
                throw new Exception("should be final: "+field);

            // it must be a AnnoMacro<? super clazz, ?>
            var fieldType = (ClassType)TypeMath.convertFromJlr(field.getAnnotatedType());
            ClassType funcType = TypeMath.getSuperType(fieldType, AnnoMacro.class);
            if(funcType==null)
                throw new Exception("not a subclass of AnnoMacro: "+funcType);
            Class<?> c1 = ((ClassType)funcType.typeArgs().get(0)).clazz();
            if(!c1.isAssignableFrom(clazz))
                throw new Exception("the converter does not accept the class type: "+field);

            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var converter = (AnnoMacro<Object,?>)field.get(null);
            return converter;
        }
    }
}
