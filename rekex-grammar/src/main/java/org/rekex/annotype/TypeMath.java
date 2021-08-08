package org.rekex.annotype;

import org.rekex.common_util.AnnoBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Some algorithms involving annotated types.
 */
public class TypeMath
{
    // convert from java.lang.reflect.AnnotatedType to our AnnoType
    public static List<AnnoType> convertFromJlr(AnnotatedType[] types)
    {
        ArrayList<AnnoType> list = new ArrayList<>(types.length);
        for(var type : types)
            list.add(convertFromJlr(type));
        return list;
    }
    public static AnnoType convertFromJlr(AnnotatedType jlrType)
    {
        // jlrType.getAnnotatedOwnerType() not looked at

        HashSet<Annotation> annotations = new HashSet<>();
        for(var anno : jlrType.getAnnotations())
        {
            // we can't trust the anno impl from JDK! use our own.
            anno = AnnoBuilder.build(anno);
            annotations.add(anno);
        }

        if(jlrType instanceof AnnotatedParameterizedType parameterizedType) // FooClass<..>
        {
            Class<?> clazz = getClassOf(parameterizedType);
            var args0 = parameterizedType.getAnnotatedActualTypeArguments();
            @SuppressWarnings("unchecked")
            List<TypeArg> typeArgs = (List<TypeArg>)(List<?>)convertFromJlr(args0);
            return new ClassType(annotations, clazz, typeArgs);
        }
        else if(jlrType instanceof AnnotatedArrayType arrayType) // including primitive arrays e.g. int[]
        {
            var compoType = convertFromJlr(arrayType.getAnnotatedGenericComponentType());
            return new ArrayType(annotations, compoType);
        }
        else if(jlrType instanceof AnnotatedTypeVariable typeVariable)
        {
            TypeVariable<?> jlrVar = (TypeVariable<?>)typeVariable.getType();
            return new TypeVar(annotations, jlrVar);
        }
        else if(jlrType instanceof AnnotatedWildcardType)
        {
            throw new UnsupportedOperationException("wildcard not supported: "+jlrType);
        }
        else // primitive, non-generic class, raw type of generic class
        {
            Class<?> clazz = getClassOf(jlrType);
            if(clazz.isPrimitive())
                return new PrimitiveType(annotations, clazz);
            else
                return new ClassType(annotations, clazz, List.of());
        }
    }

    static Class<?> getClassOf(AnnotatedType typeA)
    {
        Type type = typeA.getType();
        if(type instanceof Class<?> clazz)
            return clazz;
        else if(type instanceof ParameterizedType parameterizedType)
            return (Class<?>)parameterizedType.getRawType();
        else
            throw new IllegalArgumentException();
    }


    public static AnnoType doTypeVarSubstitution(AnnoType type, Map<TypeVar,AnnoType> map)
    {
        if(map.isEmpty()) // common
            return type;

        if(type instanceof PrimitiveType primitiveType)
        {
            return primitiveType;
        }
        else if(type instanceof ClassType classType)
        {
            var args0 = classType.typeArgs();
            if(args0.isEmpty()) // common
                return type;
            boolean changed = false;
            ArrayList<TypeArg> newArgs = new ArrayList<>(args0.size());
            for(var arg0 : args0)
            {
                RefType newArg = (RefType)doTypeVarSubstitution(arg0.asRefType(), map);
                newArgs.add(newArg);
                changed |= (newArg!=arg0);
            }
            if(!changed) // maybe common
                return type;
            return new ClassType(classType.annotations(), classType.clazz(), newArgs);
        }
        else if(type instanceof ArrayType arrayType)
        {
            AnnoType compoType = doTypeVarSubstitution(arrayType.componentType(), map);
            if(compoType==arrayType.componentType()) // common
                return type;
            return new ArrayType(arrayType.annotations, compoType);
        }
        else if(type instanceof TypeVar X)
        {
            // X  = @A V
            // V := S
            // X := @A S
            var A = X.annotations;
            var V = X;
            if(!A.isEmpty())
                V = new TypeVar(Set.of(), X.jlrVar);
            AnnoType S = map.get(V);
            if(S==null)
                throw new RuntimeException("not substitution found for type var: "+V);
            if(A.isEmpty())
                return S;
            // add A to S
            return replaceAnnos(S, union(A, S.annotations));
        }
        else
        {
            throw new AssertionError();
        }
    }


    static boolean isSubtype(AnnoType X, AnnoType Y)
    {
        var map = isInferredSubtype(X, Y);
        if(map==null)
            return false;
        if(map.isEmpty())
            return true;
        // there are type vars? the map must be identity map
        return map.entrySet().stream()
            .allMatch(e->e.getKey().equals(e.getValue()));
    }

    // X may contain type variables. If, after type inference and substitution,
    // X is a subtype of Y, return result of type inference (possibly empty).
    // otherwise return null.
    public static Map<TypeVar,AnnoType> isInferredSubtype(AnnoType X, AnnoType Y)
    {
        if(X instanceof TypeVar x)
        {
            // x may contain annotations
            return inferVarWithEquality(x, Y);
        }

        if(X instanceof ClassType x)
        {
            ClassType y = (Y instanceof ClassType ct)? ct : null;
            if(y==null)
                return null;
            if(!y.clazz.isAssignableFrom(x.clazz))
                return null;
            x = getSuperType(x, y.clazz); // could add annotations
            assert x!=null;
            if(!x.annotations.containsAll(y.annotations))
                return null;
            return inferVarWithEqualityCT(x, y);
            // equality inference is enough, since we do not support wildcards now.
        }

        if(X instanceof PrimitiveType x)
        {
            if(!X.annotations.containsAll(Y.annotations))
                return null;

            // we require exact match; ignore JLS16#4.10.1
            boolean same = (Y instanceof PrimitiveType y)
                && x.clazz==y.clazz;
            return same ? Map.of() : null;
        }

        if(X instanceof ArrayType x)
        {
            // these annotations are on the array, not on the component
            if(!X.annotations.containsAll(Y.annotations))
                return null;

            if(Y instanceof ArrayType y)
                return isInferredSubtype(x.componentType, y.componentType);
            else if(Y instanceof ClassType y && isSupertypeOfArrays(y.clazz)) // JLS16#4.10.3.
                return Map.of();
            else
                return null;
        }

        throw new AssertionError();
    }
    static boolean isSupertypeOfArrays(Class<?> clazz)
    {
        return clazz==Object.class || clazz==Cloneable.class || clazz==java.io.Serializable.class;
    }

    // return null if inference fails
    static Map<TypeVar,AnnoType> inferVarWithEquality(AnnoType sourceType, AnnoType targetType)
    {
        if(sourceType instanceof TypeVar av)
        {
            // av=(@A V), V is naked type param
            // av=targetType; infer naked V.
            // can we find S=(@B T), such that
            //   (@A @B T)==targetType, and S satisfies V's bounds?
            // if so, infer V := S
            var V = av;
            var S = targetType;
            if(!av.annotations.isEmpty())
            {
                if(!targetType.annotations.containsAll(av.annotations))
                    return null;
                HashSet<Annotation> annoB = new HashSet<>(targetType.annotations);
                annoB.removeAll(av.annotations);
                V = new TypeVar(Set.of(), av.jlrVar);
                S = replaceAnnos(targetType, annoB);
            }
            for(var bound : av.jlrVar.getAnnotatedBounds())
                if(!isSubtype(S, convertFromJlr(bound)))
                    return null;
            // good
            return Map.of(V, S);
        }

        if(!Objects.equals(sourceType.annotations, targetType.annotations))
            return null;

        if(sourceType instanceof PrimitiveType s)
        {
            boolean eq = (targetType instanceof PrimitiveType t)
                && s.clazz==t.clazz;
            return eq ? Map.of() : null;
        }
        else if(sourceType instanceof ArrayType s)
        {
            if(targetType instanceof ArrayType t)
                return inferVarWithEquality(s.componentType, t.componentType);
            else
                return null;
        }
        else if(sourceType instanceof ClassType S)
        {
            if(targetType instanceof ClassType T)
                return inferVarWithEqualityCT(S, T);
            else
                return null;
        }
        else
            throw new AssertionError();
    }
    static Map<TypeVar,AnnoType> inferVarWithEqualityCT(ClassType S, ClassType T)
    {
        // anno of S,T checked by caller

        if(S.clazz!=T.clazz)
            return null;
        assert (S.typeArgs.size()==T.typeArgs.size());
        if(S.typeArgs.isEmpty())
            return Map.of();
        HashMap<TypeVar, AnnoType> map = new HashMap<>();
        for(int i=0; i<S.typeArgs.size(); i++)
        {
            var argS = S.typeArgs.get(i).asRefType();
            var argT = T.typeArgs.get(i).asRefType();
            var m = inferVarWithEquality(argS, argT);
            if(m==null)
                return null;
            for(var entry : m.entrySet())
            {
                var v = entry.getKey();
                var t = entry.getValue();
                var prev = map.put(v, t);
                if(prev!=null && !prev.equals(t)) // conflicting results
                    return null;
            }
        }
        return map;
    }

    /**
     * Get the mapping of type variable to type argument.
     * <p>
     *     For example,
     * </p>
     * <pre>
     *     getTypeVarToArgMap(<i>List&lt;String&gt;</i>) =&gt; <i>{E-&gt;String}</i>
     * </pre>
     */
    public static Map<TypeVar,AnnoType> getTypeVarToArgMap(ClassType type)
    {
        Class<?> clazz = type.clazz;
        HashMap<TypeVar,AnnoType> map = new HashMap<>();
        var typeVars = getTypeVars(clazz);
        assert typeVars.size() == type.typeArgs.size();
        for(int i=0; i< typeVars.size(); i++)
            map.put(typeVars.get(i), type.typeArgs.get(i).asRefType());
        return map;
    }


    // should we cache the result? It's probably used a lot.
    public static List<TypeVar> getTypeVars(Class<?> clazz)
    {
        var typeParams0 = clazz.getTypeParameters();
        if(typeParams0.length==0)
            return List.of();
        ArrayList<TypeVar> list = new ArrayList<>();
        for(var tp : typeParams0)
            list.add(new TypeVar(Set.of(), tp));
        return list;
        // a type parameter may have annotations on itself.
        // they don't mean anything to us.
        // it's akin to annotations on a method parameter (not it's type)
    }

    /**
     * Get the supertype of `type` at the specified `superClass`.
     * <p>
     *     For example,
     * </p>
     * <pre>
     *     getSuperType( <i>ArrayList&lt;String&gt;</i> , Collection.class) =&gt; <i>Collection&lt;String&gt;</i>
     * </pre>
     */
    // return null if not a subclass
    public static ClassType getSuperType(ClassType type, Class<?> superClass)
    {
        Class<?> clazz = type.clazz;
        if(clazz==superClass)
            return type;

        if(!superClass.isAssignableFrom(clazz))
            return null;

        // type = A<a1..an>
        // class A<T1..Tn> extends B< s1(T1..Tn) .. sm(T1..Tn >

        // directParent = B< s1(T1..Tn) .. sm(T1..Tn) >
        AnnotatedType directParent0 = findDirectParent(clazz, superClass); // Class or ParameterizedType
        ClassType directParent = (ClassType)convertFromJlr(directParent0); // with type vars from clazz
        if(!type.annotations.isEmpty()) // add them
        {
            var annos = union(type.annotations, directParent.annotations);
            directParent = (ClassType)replaceAnnos(directParent, annos);
        }

        // do substitutions [ Ti := ai ]
        Map<TypeVar,AnnoType> map = getTypeVarToArgMap(type);
        // B< s1(a1..an) .. sm(a1..an) >
        ClassType directParent2 = (ClassType)doTypeVarSubstitution(directParent, map);

        // recursive, until superClass is reached
        return getSuperType(directParent2, superClass);
    }

    // direct parent that is also subtype of superClass
    // both args are class/interface, and thisClass is assignable to superClass
    static AnnotatedType findDirectParent(Class<?> thisClass, Class<?> superClass)
    {
        // search interfaces first. for example:
        //     class ArrayList<E> extends AbstractList<E> implements List<E> ...
        // we are most likely interested in `List<E>`, which is redundantly declared here for fast access.
        // no need to search interfaces if superClass is a class (not an interface)

        if(superClass.isInterface())
            for(var directSuperInterface : thisClass.getAnnotatedInterfaces())
                if(superClass.isAssignableFrom(getClassOf(directSuperInterface)))
                    return directSuperInterface;

        var directSuperClass = thisClass.getAnnotatedSuperclass(); // can be null
        if(directSuperClass!=null)
            if(superClass.isAssignableFrom(getClassOf(directSuperClass)))
                return directSuperClass;

        // if we are here, it must be that thisClass is Object/interface and superClass is Object. (4.10.2 )
        // return type of Object. we need somehow get an AnnotatedType of Object...
        return Void.class.getAnnotatedSuperclass(); // it should be the Object type
    }



    static AnnoType replaceAnnos(AnnoType type, Set<Annotation> newAnnos)
    {
        if(type instanceof PrimitiveType t)
            return new PrimitiveType(newAnnos, t.clazz());
        if(type instanceof ClassType t)
            return new ClassType(newAnnos, t.clazz(), t.typeArgs());
        if(type instanceof ArrayType t)
            return new ArrayType(newAnnos, t.componentType());
        if(type instanceof TypeVar t)
            return new TypeVar(newAnnos, t.jlrVar());
        throw new AssertionError();
    }
    public static AnnoType addAnno(AnnoType type, Annotation anno)
    {
        HashSet<Annotation> set = new HashSet<>(type.annotations);
        set.add(anno);
        return replaceAnnos(type, set);
    }


    static <T> Set<T> union(Set<T> s1, Set<T> s2)
    {
        if(s1.isEmpty()) return s2;
        if(s2.isEmpty()) return s1;
        HashSet<T> set = new HashSet<>();
        set.addAll(s1);
        set.addAll(s2);
        return set;
    }
}
