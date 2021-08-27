package org.rekex.grammar;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.annotype.*;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.spec.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class GrammarBuilder
{
    // it's possible that there are multiple rules with the same datatype,
    // e.g. one datatype has multiple interfaces, as sub-rule of multiple alt rules.
    ArrayList<GrammarRule> idToRule = new ArrayList<>();
    Integer newId()
    {
        idToRule.add(null);
        return idToRule.size()-1;
    }

    // only for any datatype that can be a start symbol.
    HashMap<AnnoType, Integer> typeToId = new HashMap<>();

    Class<?> ctorCatalog = null;
    List<Method> allCtorMethodsInCatalog;

    final AnnoMacro.Resolver annoResolver = new AnnoMacro.Resolver();

    void setCtorCatalog(Class<?> ctorCatalog) throws Exception
    {
        if(ctorCatalog ==null)
            throw new NullPointerException();
        checkClassAccessible(ctorCatalog);
        if(this.ctorCatalog !=null)
            throw new Exception("only one ctorCatalog is supported");
        allCtorMethodsInCatalog = getAllCtorMethodsInCatalog(ctorCatalog);
        if(allCtorMethodsInCatalog.isEmpty())
            throw new Exception("no @Ctor methods found in "+ ctorCatalog);
        this.ctorCatalog = ctorCatalog;
    }

    // this can be called recursively
    Integer findOrDerive(AnnoType type) throws Exception
    {
        Integer id = typeToId.get(type);
        if(id==null)
        {
            id = newId();
            typeToId.put(type, id);

            GrammarRule rule = doDerive(id, type);

            idToRule.set(id, rule);
        }
        return id;
    }

    GrammarRule doDerive(Integer id, AnnoType type) throws Exception
    {
        try
        {
            return doDeriveE(id, type);
        }
        catch (Exception e)
        {
            String msg = "unable to derive grammar rule for: " + str(type);
            throw new Exception(msg, e);
        }
    }
    static String str(AnnoType type)
    {
        return type.toString(false)+" (i.e. "+type.toString(true)+")";
    }

    GrammarRule doDeriveE(Integer id, AnnoType type) throws Exception
    {
        if(!isAllowedTargetType(type))
            throw new Exception("disallowed rule datatype: "+str(type));

        if(ctorCatalog !=null)
        {
            var ctorList = findCtorInCatalog(type);
            if(!ctorList.isEmpty())
                return deriveFromCtorList(id, type, ctorList);
        }

        if(type instanceof PrimitiveType primitiveType)
        {
            Class<?> clazz = primitiveType.clazz();
            if(clazz==char.class || clazz==int.class)
                return deriveRegexStr(id, primitiveType);

            throw new Exception("no predefined rule for primitive: "+str(type));
        }
        else if(type instanceof ClassType classType)
        {
            Class<?> clazz = classType.clazz();
            if(clazz==Character.class || clazz==Integer.class || clazz==String.class)
                return deriveRegexStr(id, classType);
            if(clazz==List.class)
                return deriveList(id, classType);
            if(clazz==Peek.class)
                return derivePeek(id, classType);
            if(clazz==Not.class)
                return deriveNot(id, classType);

            return deriveClassType(id, classType);
        }
        else if(type instanceof ArrayType arrayType)
        {
            return deriveArray(id, arrayType);
        }
        else
        {
            throw new AssertionError();
        }
    }


    // String, char, Character,
    GrammarRule deriveRegexStr(Integer id, AnnoType type) throws Exception
    {
        var listRegex = getRegex(type.annotations());
        if(listRegex.size()==0)
            throw new Exception("no @Regex on: "+str(type));
        if(listRegex.size()>1)
            throw new Exception("multiple @Regex on: "+str(type));
        return new RegexRule(id, type, listRegex.get(0), null);
    }

    // there should be one and only one regex-equivalent annotation.
    // (multiple anno may be supported in future, as intersection of them; but it's confusing)
    // non-Regex annotations are skipped here; no error.
    List<Regex> getRegex(Set<Annotation> annotations) throws Exception
    {
        ArrayList<Regex> list = new ArrayList<>();
        for(var anno : annotations)
            if(annoResolver.resolve(anno) instanceof Regex r)
                list.add(validateRegex(r));
        return list;
    }
    Regex validateRegex(Regex r) throws Exception
    {
        try
        {
            var pattern = Pattern.compile(r.value(), r.flags()); // throws
            var matcher = pattern.matcher("");
            if(r.group()>matcher.groupCount())
                throw new Exception("invalid group number");
            return r;
        }
        catch (Exception exception)
        {
            throw new Exception("invalid @Regex: "+r, exception);
        }
    }




    RepeatRule deriveList(Integer id, ClassType classType) throws Exception
    {
        var componentType = classType.typeArgs().get(0).asRefType();
        return deriveRepeatRule(id, classType, componentType);
    }
    RepeatRule deriveArray(Integer id, ArrayType arrayType) throws Exception
    {
        // note that, in the following code
        //     @Regex("ab")char @AtLeast(1) []
        // @Regex applies on `char` itself; @AtLeast applies on the whole array.
        // we don't support  char @Regex("ab") []. use @Regex on String instead.

        var componentType = arrayType.componentType();
        return deriveRepeatRule(id, arrayType, componentType);
    }
    RepeatRule deriveRepeatRule(Integer id, AnnoType aggType, AnnoType compoType) throws Exception
    {
        // multiple SizeLimit annos are intersected
        int min = 0;
        int max = Integer.MAX_VALUE;
        for(var anno : aggType.annotations())
        {
            if(annoResolver.resolve(anno) instanceof SizeLimit s)
            {
                if(!(0<=s.min() && s.min()<=s.max()))
                    throw new Exception("incorrect @SizeLimit annotation: min="
                        +s.min()+", max="+s.max()+", annotation: "+anno);
                min = Math.max(min, s.min());
                max = Math.min(max, s.max());
            }
            // else, ignore
        }
        if(!(min<=max))
            throw new Exception("incorrect @SizeLimit: min="+min+", max="+max);

        Integer subRuleId = findOrDerive(compoType);
        return new RepeatRule(id, aggType, subRuleId, min, max);
    }



    PeekRule derivePeek(Integer id, ClassType classType) throws Exception
    {
        var compoType = classType.typeArgs().get(0).asRefType();
        Integer subRuleId = findOrDerive(compoType);
        return new PeekRule(id, classType, subRuleId);
    }
    NegRule deriveNot(Integer id, ClassType classType) throws Exception
    {
        var compoType = classType.typeArgs().get(0).asRefType();
        Integer subRuleId = findOrDerive(compoType);
        return new NegRule(id, classType, subRuleId);
    }


    void checkClassAccessible(Class<?> clazz) throws Exception
    {
        boolean topLevel = clazz.getEnclosingClass()==null;
        int mods = clazz.getModifiers();
        if(!(topLevel ||Modifier.isStatic(mods)))
            throw new Exception("class must be top-level or static: "+clazz);
        if(!Modifier.isPublic(mods))
            throw new Exception("class must be public: "+clazz);
    }

    GrammarRule deriveClassType(Integer id, ClassType classType) throws Exception
    {
        Class<?> clazz = classType.clazz();
        checkClassAccessible(clazz);

        //   @Ctor on constructors/methods declared in class
        {
            var ctorList = findCtorListIn(clazz, classType);
            if (!ctorList.isEmpty())
                return deriveFromCtorList(id, classType, ctorList);
        }

        // enum
        // @Permits and permits won't work on enum, because there's no named subclasses
        // @Ctor methods could be declared in enum{} which is handled by previous logic
        //   if none, derived rules for enum constants
        if(clazz.isEnum())
        {
            return deriveEnum(id, classType);
            // enum types are sealed or final. this clause precedes the sealed clause.
        }

        //   @Permits() on class
        {
            Permits permits = clazz.getAnnotation(Permits.class);
            if(permits!=null)
            {
                validatePermitsAnno(clazz, permits);
                return deriveAltSubClasses(id, classType, permits.value());  // ordered
            }
        }

        // sealed class without @Permits
        if(clazz.isSealed())
        {
            var subclasses = clazz.getPermittedSubclasses();
            PkgUtil.orderSealedSubclasses(clazz, subclasses);  // throws
            return deriveAltSubClasses(id, classType, subclasses);
        }

        // class with a single non-default public constructor
        {
            var ctorList = findPublicConstructors(clazz, classType);
            if(ctorList.size()==1)
            {
                if(ctorList.get(0).ctor.getParameterCount()>0)
                    return deriveFromCtorList(id, classType, ctorList);
                // if the class has a single default constructor,
                // it's likely accidental, not intended as a ctor (of epsilon).
                // If it is actually intended, mark it as @Ctor.
                throw new Exception("found only a default constructor; mark it as @Ctor if needed: "+clazz);
            }

            // if we are here, we don't know what user wants. give some hints?
            throw new Exception("expecting a single non-default public constructor: "+clazz);
        }

    }

    GrammarRule deriveEnum(Integer id, ClassType enumClassType) throws Exception
    {
        // annotations declared on clazz are not type-annotations
        Class<?> clazz = enumClassType.clazz();
        var enumObjs = clazz.getEnumConstants();
        if(enumObjs.length==0)
        {
            // an empty enum type. we could allow it, which means it never matches.
            // but it's very likely a user error, so we reject it.
            // if user does want "never", try workarounds like Not<Epsilon>
            throw new Exception("enum is empty: " + clazz);
        }

        // annotations of enumClassType are not checked;
        // maybe they could be used to filter fields without them.

        ArrayList<Integer> subRuleIds = new ArrayList<>(enumObjs.length);

        for(Object ec : enumObjs)
        {
            Enum<?> em = (Enum<?>)ec;
            var field = clazz.getDeclaredField(em.name());
            if(!field.canAccess(null)) // hmm?
                throw new Exception("can not access field: "+field);

            Set<Annotation> annotations = enumFieldAnnos(field);
            var listRegex = getRegex(annotations);
            if(listRegex.size()==0)
                throw new Exception("no @Regex on field: "+field.getName()+" in "+clazz);
            if(listRegex.size()>1)
                throw new Exception("multiple @Regex on field: "+field.getName()+" in "+clazz);

            // datatype of the rule contains field anno; mostly for diagnosis
            Set<Annotation> typeAnno = enumObjs.length==1 ? Set.of() : annotations;
            ClassType dataType = new ClassType(typeAnno, clazz, List.of());
            Integer subId = newId();
            Instantiator instantiator = new Instantiator.StaticField(field);
            GrammarRule rule = new RegexRule(subId, dataType, listRegex.get(0), instantiator);

            idToRule.set(subId, rule);
            subRuleIds.add(subId);
        }
        return new AltRule(id, enumClassType, subRuleIds);
    }
    Set<Annotation> enumFieldAnnos(Field field)
    {
        // System.out.println(field.getAnnotatedType()); // no anno ?

        // it's unclear why these annotations with @Target(TYPE_USE)
        // are considered declaration annotations on the field,
        // but not type annotations on the field type.

        // we can't trust the anno impl from JDK! use our own.

        return Set.of(field.getDeclaredAnnotations())
            .stream().map(AnnoBuilder::build) // change anno to our impl
            .collect(Collectors.toSet());
    }



    GrammarRule deriveAltSubClasses(Integer id, ClassType classType, Class<?>[] subclasses) throws Exception
    {
        if(subclasses.length==0) // won't reached
            throw new Exception("no subclasses for "+str(classType));

        ArrayList<Integer> subRuleIds = new ArrayList<>();
        for(var subclass : subclasses)
        {
            // annotations declared on subclass are not type-annotations
            // subclass can extend annotated superclass
            var subTypeV = ClassType.withTypeVars(subclass);
            subTypeV = new ClassType(classType.annotations(), subTypeV.clazz(), subTypeV.typeArgs());

            // solve type variables in the subtype
            var varMap = TypeMath.isInferredSubtype(subTypeV, classType);

            if(varMap==null)
                throw new Exception("cannot infer [1] to be [2]: [1]="+subclass+ " [2]="+classType);
            // it is conceivable that we allow it, as a filter on subclasses. e.g.
            //     Sub implements Sup<Foo>, unqualified for target Sup<Bar>
            // for now, treat it as a programming error.

            var subType = TypeMath.doTypeVarSubstitution(subTypeV, varMap);

            var subRuleId = findOrDerive(subType);
            subRuleIds.add(subRuleId);
        }
        return new AltRule(id, classType, subRuleIds);
    }



    GrammarRule deriveFromCtorList(Integer id, AnnoType targetType, List<CtorInfo> ctorList) throws Exception
    {
        assert ctorList.size()>0;
        ArrayList<Integer> subRuleIds = new ArrayList<>();

        // these subRules are not addressable by their return types. e.g.
        //     class Foo
        //         FooA ctorA(...)
        // FooA could as well appear in another ctor in another class.
        // if user wants to use FooA as a start symbol, it's inappropriate to invoke this subRule.
        // the rule for FooA should be derivable from FooA itself.

        int N = ctorList.size();
        for(int i=0; i<N; i++)
        {
            var ctor = ctorList.get(i);
            Integer subId = newId();
            GrammarRule rule;
            try
            {
                rule = deriveCtor(subId, ctor, N>1 ? i : -1);
            }
            catch (Exception e)
            {
                throw new Exception("unable to derive rule for ctor: "+ctor.ctor, e);
            }
            idToRule.set(subId, rule);
            subRuleIds.add(subId);
        }

        return new AltRule(id, targetType, subRuleIds);
    }

    GrammarRule deriveCtor(Integer id, CtorInfo info, int ctorIndex) throws Exception
    {
        AnnoType returnType = info.returnType;
        Executable ctor = info.ctor;
        Map<TypeVar, AnnoType> typeVarMap = info.typeVarMap;

        ArrayList<Integer> subRuleIds = new ArrayList<>();
        var paramTypes = TypeMath.convertFromJlr(ctor.getAnnotatedParameterTypes());
        for(var paramType : paramTypes)
        {
            // fail if some vars are not substituted
            var paramType2 = TypeMath.doTypeVarSubstitution(paramType, typeVarMap);
            var subRuleId = findOrDerive(paramType2);
            subRuleIds.add(subRuleId);
        }

        Instantiator instantiator;
        if(ctor instanceof Constructor<?> constructor)
            instantiator = new Instantiator.NewInstance(constructor);
        else if(ctor instanceof Method method)
            instantiator = new Instantiator.StaticMethod(method);
        else
            throw new AssertionError();

        // concat. arity 0, 1, 2... are all useful and allowed.
        return new ConcatRule(id, returnType, subRuleIds, instantiator);
    }



    // not for types that's too "simple" or "basic"
    // at least, it's confusing to allow un-annotated char/String types.
    boolean isAllowedTargetType(AnnoType type)
    {
        if(type instanceof TypeVar) // uh?
            return false;

        if(!type.annotations().isEmpty())
            return true;  // ok, it's a "sophisticated" type

        // bare type, no annotations

        if(type instanceof PrimitiveType)
            return false;

        if(type instanceof ArrayType) // ok
            return true;

        if(type instanceof ClassType ct)
        {
            // disallow anything in java.lang
            var pkg = ct.clazz().getPackageName();
            return(!pkg.equals("java.lang"));
        }

        throw new AssertionError();
    }

    void validatePermitsAnno(Class<?> clazz, Permits permits) throws Exception
    {
        try
        {
            HashSet<Class<?>> set = new HashSet<>();
            for(var c : permits.value())
            {
                if(!set.add(c))
                    throw new Exception("duplicate classes: "+ c);
                if(c== clazz || !clazz.isAssignableFrom(c))
                    throw new Exception("[1] not a proper subclass of [2]: [1]="+c+" [2]="+clazz);
            }
            if(set.isEmpty())
                throw new Exception("@Permits contains no classes");
            if(clazz.isSealed())
            {
                var set2 = Set.of(clazz.getPermittedSubclasses());
                // require that they are identical. @Permits is likely used for ordering
                if(!set.equals(set2)) // likely a programming error
                    throw new Exception("@Permits annotation inconsistent with permits clause of the sealed class");
            }
        }
        catch (Exception e)
        {
            throw new Exception("invalid @Permits[1] on class[2]: [1]="+permits+" [2]="+clazz, e);
        }
    }





    record CtorInfo(AnnoType returnType, Executable ctor, Map<TypeVar, AnnoType> typeVarMap){}

    CtorInfo tryInferCtor(Method method, AnnoType targetType)
    {
        var returnType = TypeMath.convertFromJlr(method.getAnnotatedReturnType());
        var typeVarMap = TypeMath.isInferredSubtype(returnType, targetType);
        if(typeVarMap==null)
            return null;
        returnType = TypeMath.doTypeVarSubstitution(returnType, typeVarMap);
        return new CtorInfo(returnType, method, typeVarMap);
    }
    CtorInfo newConstructorCtor(Constructor<?> ctor, ClassType classType)
    {
        var typeVarMap = TypeMath.getTypeVarToArgMap(classType);
        // the constructor itself may also have type parameters, which are not solved.
        // it's a quite rare use case; we'll take care of it if necessary.
        return new CtorInfo(classType, ctor, typeVarMap);
    }

    // two ctor methods could have the same parameter signatures
    // (made more likely because method signature only cares about the raw type)
    // that's not a problem because we can always choose different method names.
    //
    // it's probably cleaner if ctors are instance methods of an interface
    // without the need of modifiers of @Ctor public static; simply
    //    Foo foo(Bar bar);
    // the interface is thus a very clean catalog of rules. TBA.

    List<CtorInfo> findCtorInCatalog(AnnoType type) throws Exception
    {
        ArrayList<CtorInfo> ctorList = new ArrayList<>();
        for(var ctor : allCtorMethodsInCatalog)
        {
            var info = tryInferCtor(ctor, type);
            if(info!=null)
                ctorList.add(info);
        }

        return PkgUtil.orderCtors(ctorList); // throws
    }
    List<Method> getAllCtorMethodsInCatalog(Class<?> ctorCatalog) throws Exception
    {
        try
        {
            return getAllCtorMethodsInCatalogEx(ctorCatalog);
        }
        catch (Exception e)
        {
            throw new Exception("error finding ctors in rule catalog: "+ctorCatalog, e);
        }
    }
    List<Method> getAllCtorMethodsInCatalogEx(Class<?> ctorCatalog) throws Exception
    {
        ArrayList<Method> ctorList = new ArrayList<>();
        for(var ctor : ctorCatalog.getDeclaredMethods()) // no inherited methods?
        {
            // @Ctor public static
            if(!ctor.isAnnotationPresent(Ctor.class))
                continue;
            if(!Modifier.isPublic(ctor.getModifiers()))
                throw new Exception("ctor must be public: "+ctor);
            if(!Modifier.isStatic(ctor.getModifiers()))
                throw new Exception("ctor must be static: "+ctor);

            ctorList.add(ctor);
        }
        return ctorList;
    }

    List<CtorInfo> findCtorListIn(Class<?> clazz, ClassType classType) throws Exception
    {
        try
        {
            return findCtorListInEx(clazz, classType);
        }
        catch (Exception e)
        {
            throw new Exception("error finding ctors in "+clazz, e);
        }
    }
    List<CtorInfo> findCtorListInEx(Class<?> clazz, ClassType classType) throws Exception
    {
        ArrayList<CtorInfo> ctorList = new ArrayList<>();
        for(var ctor : clazz.getDeclaredConstructors())
        {
            if(!ctor.isAnnotationPresent(Ctor.class))
                continue;

            ctorList.add(newConstructorCtor(ctor, classType));
        }
        for(var ctor : clazz.getDeclaredMethods())
        {
            if(!ctor.isAnnotationPresent(Ctor.class))
                continue;

            if(!Modifier.isStatic(ctor.getModifiers()))
                throw new Exception("ctor must be static: "+ctor);

            var info = tryInferCtor(ctor, classType);

            if(info==null)
                throw new Exception("return type of ctor[1] not a subtype of [2]: [1]="+ctor+" [2]="+classType);
            // it is conceivable that return types can be used to filter ctors,
            // e.g. ctor returning Foo<A> doesn't cater to target type Foo<B>
            // for now, let's treat it as a programming mistake.

            ctorList.add(info);
        }
        for(var ctor : ctorList)
        {
            if(!Modifier.isPublic(ctor.ctor().getModifiers()))
                throw new Exception("ctor must be public: "+ctor);
            if(!ctor.ctor().canAccess(null))
                throw new Exception("can not access ctor: "+ctor);
        }

        return PkgUtil.orderCtors(ctorList); // throws
    }

    List<CtorInfo> findPublicConstructors(Class<?> clazz, ClassType classType) throws Exception
    {
        ArrayList<CtorInfo> ctorList = new ArrayList<>();
        for(var ctor : clazz.getConstructors())
        {
            if(!ctor.canAccess(null))
                throw new Exception("can not access ctor: "+ctor);

            ctorList.add(newConstructorCtor(ctor, classType));
        }
        return PkgUtil.orderCtors(ctorList); // throws
    }



}
