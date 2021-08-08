package org.rekex.grammar;

import org.rekex.annotype.AnnoType;
import org.rekex.common_util.EscapeJavaChar;
import org.rekex.common_util.SwitchOnType;
import org.rekex.regexp.RegExpApi;
import org.rekex.spec.Regex;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

class ToDumpRules
{
    static void dump(List<GrammarRule> idToRule, Map<AnnoType, Integer> typeToId)
    {
        dump(idToRule, typeToId, System.out::print);
        System.out.println();
    }

    static void dump(List<GrammarRule> idToRule, Map<AnnoType, Integer> typeToId, Consumer<String> out)
    {
        final String NL = System.lineSeparator();
        for(var rule : idToRule)
        {
            boolean canRoot = rule.id().equals(typeToId.get(rule.datatype()));
            String idStr = canRoot
                ? "[%d] ".formatted(rule.id())
                : "(%d) ".formatted(rule.id());
            out.accept(idStr+ rule.datatype().toString(false));

            String indent = NL + " ".repeat(2+idStr.length());
            for(var line : ruleToLines.apply(rule))
            {
                if(line.isEmpty()) continue;
                out.accept(indent);
                out.accept(line);
            }
            out.accept(NL);
            out.accept(NL);
        }
    }
    static final Function<GrammarRule,List<String>> ruleToLines =
        new SwitchOnType<GrammarRule, List<String>>()
            .on(AltRule.class, r->List.of(
                join("alt( ", " | ", " )", r.subRuleIds())
            ))
            .on(ConcatRule.class ,r->List.of(
                join("seq[", ", ", "]", r.subRuleIds()),
                toStr(r.instantiator())
            ))
            .on(RepeatRule.class, r->List.of(
                "rep["+r.subRuleId()+"]",
                r.min()==0 ? "" : "min: "+r.min(),
                r.max()==Integer.MAX_VALUE ? "" : "max: "+r.max()
            ))
            .on(PeekRule.class, r->List.of(
                "peek["+r.subRuleId()+"]"
            ))
            .on(NegRule.class, r->List.of(
                "neg["+r.subRuleId()+"]"
            ))
            .on(RegexRule.class, r->List.of(
                r.instantiator()==null ? "" : toStr(r.instantiator()),
                "regex: "+ escRegex(r.regex()),
                r.regex().group()==0 ? "" : "group: "+r.regex().group(),
                r.regex().flags()==0 ? "" : "flags: "+r.regex().flags()
            ))
        .complete(GrammarRule.class);

    static String join(String prefix, String delimiter, String suffix, List<?> list)
    {
        return list.stream()
            .map(Object::toString)
            .collect(Collectors.joining(delimiter, prefix, suffix));
    }

    static String escRegex(Regex r)
    {
        var exp = RegExpApi.opaque(r.value());
        var str = RegExpApi.toRegex(exp); // esc by regex rules
        return EscapeJavaChar.asInJavaSource(str);
    }

    static final Function<Instantiator,String> insToStr =
        new SwitchOnType<Instantiator,String>()
            .on(Instantiator.NewInstance.class, x->toStr(x.constructor()))
            .on(Instantiator.StaticMethod.class, x->toStr(x.method()))
            .on(Instantiator.StaticField.class, x->toStr(x.field()))
            .complete(Instantiator.class);
    static String toStr(Instantiator instantiator)
    {
        return insToStr.apply(instantiator);
    }
    static String toStr(Constructor<?> constructor)
    {
        var clazz = constructor.getDeclaringClass();
        var paramTypes = constructor.getParameterTypes();
        return "ctor: new %s(%s)".formatted(toStr(clazz), toStr(paramTypes));
    }
    static String toStr(Method method)
    {
        var clazz = method.getDeclaringClass();
        var name = method.getName();
        var paramTypes = method.getParameterTypes();
        return "ctor: %s.%s(%s)".formatted(toStr(clazz), name, toStr(paramTypes));
    }
    static String toStr(Field field)
    {
        var clazz = field.getDeclaringClass();
        var name = field.getName();
        return "field: %s.%s".formatted(toStr(clazz), name);
    }
    static String toStr(Class<?>[] types)
    {
        return List.of(types).stream()
            .map(clazz-> toStr(clazz))
            .collect(Collectors.joining(","));
    }

    static String toStr(Class<?> clazz)
    {
        if(clazz.isArray())
            return toStr(clazz.getComponentType())+"[]";
        String name = clazz.getSimpleName();
        if(name.isEmpty()) // ??
            name = clazz.getName();
        return name;
    }

}
