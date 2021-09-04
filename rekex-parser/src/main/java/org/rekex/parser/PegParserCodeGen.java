package org.rekex.parser;

import org.rekex.annotype.AnnoType;
import org.rekex.annotype.ArrayType;
import org.rekex.annotype.ClassType;
import org.rekex.annotype.PrimitiveType;
import org.rekex.codegen.Imports;
import org.rekex.common_util.EscapeJavaChar;
import org.rekex.common_util.SwitchOnType;
import org.rekex.grammar.*;
import org.rekex.spec.Not;
import org.rekex.spec.Peek;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class PegParserCodeGen
{
    public static void generate(AnnoType rootType, Grammar grammar, String packageName, String className, Appendable out) throws Exception
    {
        PegParserCodeGen thiz = new PegParserCodeGen(rootType, grammar);
        thiz.generate(packageName, className, out);
    }


    PegParserMaker maker = new PegParserMaker();
    AnnoType rootType;
    Grammar grammar;

    PegParserCodeGen(AnnoType rootType, Grammar grammar)
    {
        this.rootType = rootType;
        this.grammar = grammar;
    }

    boolean match_repeat_list;
    boolean match_repeat_obj_array;
    boolean match_repeat_prim_array;
    boolean regexToStr;
    boolean regexToChar;
    boolean regexToInt;
    boolean regexToField;

    void generate(String packageName, String className, Appendable out) throws Exception
    {
        Object typeArg = typeStr( (rootType instanceof PrimitiveType pt) ? pt.boxed() : rootType );

        maker.fileHeader1(packageName, lazy(this::importLines), className, typeArg,
            annoTypeStr(rootType), grammar.typeToId().get(rootType));

        var catalogClass = grammar.catalogClass()!=null
            ? classStr(grammar.catalogClass())
            : classStr(java.lang.Object.class);
        maker.fileHeader2(catalogClass);
        if(grammar.catalogClass()==null)
            maker.fileHeader2A(className);
        else
            maker.fileHeader2B(className, catalogClass);

        maker.fileHeader3(typeArg, catalogClass);

        maker.matchAnyRuleIdHeader();
        for(var rule : grammar.idToRule())
            maker.matchAnyRuleIdCase(rule.id(), annoTypeStr(rule.datatype()));
        maker.matchAnyRuleIdFooter();

        var f = new SwitchOnType<GrammarRule, Void>()
            .on(AltRule.class, this::genAlt)
            .on(ConcatRule.class, this::genConcat)
            .on(RepeatRule.class, this::genRepeat)
            .on(PeekRule.class, this::genPeek)
            .on(NegRule.class, this::genNeg)
            .on(RegexRule.class, this::genRegex)
            .complete(GrammarRule.class);

        for(var rule : grammar.idToRule())
            f.apply(rule);

        maker.helperMethodsHeader();

        if(match_repeat_obj_array)
            maker.generic_array();
        if(match_repeat_list || match_repeat_obj_array || match_repeat_prim_array)
            maker.match_repeat();


        maker.match_regex(); // always needed
        if(regexToChar)
            maker.match_regex_char();
        if(regexToChar || regexToInt)
            maker.match_regex_int();
        if(regexToStr)
            maker.match_regex_str();
        if(regexToField)
            maker.match_regex_obj();

        maker.datatypeListHeader();
        for(var rule : grammar.idToRule())
            maker.datatypeField(rule.datatype().toString(true), rule.datatype().toString(false), rule.id());
        maker.datatypeListFooter(grammar.idToRule().size());

        maker.fileFooter();

        Set<Class<?>> preImports = Set.of(
            PegParser.class,
            ParseResult.class,
            // nested classes; as if those classes are imported
            PegParserTemplate._State.class,
            PegParserTemplate._FatalEx.class,
            PegParserTemplate._DatatypeList.class
        );
        this.imports = Imports.build(preImports, referencedClasses);

        maker.writeTo(out);
    }

    Object annoTypeStr(AnnoType t)
    {
        return t.toString(false);
    }

    Object lazy(Supplier<String> toStr)
    {
        return new Object()
        {
            public String toString(){ return toStr.get(); }
        };
    }

    Object typeStr(int arr, Class<?> clazz, List<Object> typeArgs)
    {
        return lazy(()->{
            String s = imports.resolve(clazz);
            if(!typeArgs.isEmpty())
            {
                List<String> ls = typeArgs.stream().map(Object::toString).toList();
                s += "<" + String.join(", ", ls) + ">";
            }
            s += "[]".repeat(arr);
            return s;
        });
    }
    Object typeStr(AnnoType t)
    {
        int arr=0;
        while(t instanceof ArrayType a)
        {
            arr++;
            t = a.componentType();
        }
        if(t instanceof PrimitiveType pt)
            return typeStr(arr, pt.clazz(), List.of());
        ClassType ct = (ClassType)t;
        referencedClasses.add(ct.clazz());
        List<Object> typeArgs = ct.typeArgs().stream().map(a->typeStr(a.asRefType())).toList();
        return typeStr(arr, ct.clazz(), typeArgs);
    }
    Object classStr(Class<?> clazz)
    {
        referencedClasses.add(clazz);
        return typeStr(0, clazz, List.of());
    }

    HashSet<Class<?>> referencedClasses = new HashSet<>();
    Imports imports = null; // created in the end
    String importLines()
    {
        return imports.imports().stream()
            .map(cn->"import "+cn+";\n")
            .collect(Collectors.joining());
    }

    Void genAlt(AltRule rule)
    {
        maker.matchAltHeader(rule.id(), annoTypeStr(rule.datatype()));

        int N = rule.subRuleIds().size();
        if(N==0) throw new AssertionError(); // the template requires N>0
        for(int i=0; i<N; i++)
        {
            var subId = rule.subRuleIds().get(i);
            var subRule = grammar.idToRule().get(subId);
            var datatype = subRule.datatype();
            maker.matchAltSubRule(subId, i, annoTypeStr(datatype));
        }

        maker.matchAltFooter();
        return null;
    }

    Void genConcat(ConcatRule rule)
    {
        maker.matchConcatHeader(rule.id(), annoTypeStr(rule.datatype()));

        int N = rule.subRuleIds().size();
        for(int i=0; i<N; i++)
        {
            var subId = rule.subRuleIds().get(i);
            var subRule = grammar.idToRule().get(subId);
            var datatype = subRule.datatype();
            maker.matchConcatSubRule(i, subId, typeStr(datatype), annoTypeStr(datatype));
        }

        String args = IntStream.range(0,N).mapToObj(i->"arg_"+i).collect(Collectors.joining(", "));

        maker.instantiateHeader(typeStr(rule.datatype()));
        if(rule.instantiator() instanceof Instantiator.NewInstance)
        {
            maker.instantiateNewInstance(typeStr(rule.datatype()), args);
        }
        else if(rule.instantiator() instanceof Instantiator.StaticMethod m)
        {
            maker.instantiateStaticMethod(classStr(m.method().getDeclaringClass()), m.method().getName(), args);
        }
        else if(rule.instantiator() instanceof Instantiator.InstanceMethod m)
        {
            maker.instantiateInstanceMethod(m.method().getName(), args);
        }
        else
            throw new AssertionError();

        var declaredEx = PkgUtil.getDeclaredExceptions(rule.instantiator());
        referencedClasses.addAll(declaredEx);
        if(declaredEx.contains(Exception.class))
        {
            maker.instantiateEx1(classStr(Exception.class), rule.id());
        }
        else if(declaredEx.isEmpty())
        {
            maker.instantiateEx2();
        }
        else // throws subclasses of Exception
        {
            var catchTypeString = lazy(()->
                declaredEx.stream().map(c->imports.resolve(c)).collect(Collectors.joining(" | "))
            );
            maker.instantiateEx1(catchTypeString, rule.id());
            maker.instantiateEx2();
        }

        maker.matchConcatFooter();
        return null;
    }

    Void genRepeat(RepeatRule rule)
    {
        Object datatypeStr = annoTypeStr(rule.datatype());
        var compoType = grammar.idToRule().get(rule.subRuleId()).datatype();
        var compoTypeStr = typeStr(compoType);
        Object min = rule.min();
        Object max = rule.max()==Integer.MAX_VALUE ? "Integer.MAX_VALUE" : rule.max();
        if(rule.datatype() instanceof ArrayType)
        {
            if(compoType instanceof PrimitiveType pt)
            {
                match_repeat_prim_array =true;
                maker.match_repeat_prim_array(rule.id(), rule.id(), datatypeStr,
                    rule.subRuleId(), compoTypeStr, min, max, typeStr(pt.boxed()));
            }
            else
            {
                match_repeat_obj_array =true;
                maker.match_repeat_obj_array(rule.id(), rule.id(), datatypeStr,
                    rule.subRuleId(), compoTypeStr, min, max);
            }
        }
        else // List
        {
            match_repeat_list = true;
            maker.match_repeat_list(rule.id(), rule.id(), datatypeStr,
                rule.subRuleId(), compoTypeStr, min, max);
        }
        return null;
    }

    Void genPeek(PeekRule rule)
    {
        referencedClasses.add(Peek.class);

        Object datatypeStr = annoTypeStr(rule.datatype());
        var compoType = grammar.idToRule().get(rule.subRuleId()).datatype();
        var compoTypeStr = typeStr(compoType);
        maker.match_peek(rule.id(), datatypeStr, rule.subRuleId(), compoTypeStr);
        return null;
    }

    Void genNeg(NegRule rule)
    {
        referencedClasses.add(Not.class);

        Object datatypeStr = annoTypeStr(rule.datatype());
        maker.match_neg(rule.id(), rule.id(), datatypeStr, typeStr(rule.datatype()), rule.subRuleId());
        return null;
    }

    Void genRegex(RegexRule rule)
    {
        int ruleId = rule.id();
        Object datatypeStr = annoTypeStr(rule.datatype());
        int group = rule.regex().group();
        String regexStr = EscapeJavaChar.asInJavaSource(rule.regex().value());

        if(rule.instantiator()!=null && rule.instantiator() instanceof Instantiator.StaticField sf)
        {
            regexToField=true;
            var field = sf.field();
            maker.matchRegexToField(ruleId, ruleId, datatypeStr, group,
                classStr(field.getDeclaringClass()), field.getName());
        }
        else if(rule.datatype() instanceof PrimitiveType pt && pt.clazz()==char.class
            ||  rule.datatype() instanceof ClassType ct && ct.clazz()==Character.class)
        {
            regexToChar=true;
            maker.matchRegexToChar(ruleId, ruleId, datatypeStr, group);
        }
        else if(rule.datatype() instanceof PrimitiveType pt && pt.clazz()==int.class
            ||  rule.datatype() instanceof ClassType ct && ct.clazz()==Integer.class)
        {
            regexToInt=true;
            maker.matchRegexToInt(ruleId, ruleId, datatypeStr, group);
        }
        else if(rule.datatype() instanceof ClassType ct && ct.clazz()==String.class)
        {
            regexToStr=true;
            maker.matchRegexToStr(ruleId, ruleId, datatypeStr, group);
        }
        else
        {
            throw new AssertionError("unexpected rule datatype: "+rule.datatype());
        }
        maker.patternField(ruleId, regexStr, rule.regex().flags());

        return null;
    }

}
