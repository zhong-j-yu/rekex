package org.rekex.grammar;

import org.rekex.annotype.AnnoType;
import org.rekex.common_util.SwitchOnType;

import java.util.*;
import java.util.function.Function;

/**
 * A grammar is a collection of grammar rules.
 */
public class Grammar
{
    final List<GrammarRule> idToRule;
    final Map<AnnoType, Integer> typeToId;

    Grammar(List<GrammarRule> idToRule, Map<AnnoType, Integer> typeToId)
    {
        this.idToRule = idToRule;
        this.typeToId = typeToId;
    }

    /**
     * All rules of this grammar.
     * Ids of rules are consecutive from 0 to n-1,
     * consistent with their index in the returned List.
     * Therefore `idToRule().get(ruleId)` returns the rule with the ruleId.
     */
    public List<GrammarRule> idToRule()
    {
        return idToRule;
    }

    /**
     * Return a mapping from datatype to rule id.
     */
    public Map<AnnoType, Integer> typeToId()
    {
        return typeToId;
    }

    /**
     * Return a textual description of the grammar rules.
     */
    public String toText()
    {
        StringBuilder sb = new StringBuilder();
        ToDumpRules.dump(idToRule, typeToId, sb::append);
        return sb.toString();
    }

    /**
     * Derive a grammar, starting from the given datatypes, and optionally with a ctor catalog.
     */
    public static Grammar deriveFrom(List<AnnoType> types, Class<?> ctorCatalog)
    {
        try
        {
            return deriveFromX(types, ctorCatalog);
        }
        catch (Exception exception)
        {
            throw new RuntimeException("failed to derive grammar", exception);
        }
    }

    static Grammar deriveFromX(List<AnnoType> types, Class<?> ctorCatalog) throws Exception
    {
        GrammarBuilder gb = new GrammarBuilder();

        if(ctorCatalog!=null)
            gb.setCtorCatalog(ctorCatalog);

        ArrayList<Integer> rootIds = new ArrayList<>();
        for(var type : types)
            rootIds.add(gb.findOrDerive(type));

        // if AltRule `a` has a single subRule `s`
        //    if `a` and `s` are the same datatype, `a` is just redundant.
        //        replace all reference to `a` to reference to `s`.
        //        remove `a`. compact ids due to the removals.
        //    if `a` and `s` are different datatypes, keep the relationship.
        //        user may want to see it in diagnosis. e.g. sealed class -> single subclass

        var idToRule = gb.idToRule;
        HashMap<Integer,Integer> equivMap = new HashMap<>();
        for(int id=0; id<idToRule.size(); id++)
        {
            var rule = idToRule.get(id);
            int equivId = findEquivalent(rule, idToRule).id();
            equivMap.put(id, equivId);
        }

        // rearrange ids BFS, such that subrule ids are likely adjacent.
        Integer[] idMap = new Integer[idToRule.size()];
        int idSeq = 0;
        ArrayDeque<Integer> queue = new ArrayDeque<>(rootIds);
        while(!queue.isEmpty())
        {
            Integer id = queue.removeFirst();
            if(idMap[id]!=null)
                continue;

            Integer idE = equivMap.get(id); // could be the same id
            if(idMap[idE]!=null)
            {
                idMap[id] = idMap[idE];
                continue;
            }

            var newId = idSeq++;
            idMap[id] = idMap[idE] = newId;
            queue.addAll(getSubIds.apply(idToRule.get(idE)));
        }

        ArrayList<GrammarRule> new_idToRule = new ArrayList<>(idSeq);
        for(int i=0; i<idSeq; i++) new_idToRule.add(null);
        for (var rule : idToRule)
        {
            var newRule = replaceIds.apply(rule).apply(idMap);
            new_idToRule.set(newRule.id(), newRule);
        }

        HashMap<AnnoType, Integer> new_typeToId = new HashMap<>();
        gb.typeToId.forEach((type,id)->
            new_typeToId.put(type, idMap[id]));

        return new Grammar(new_idToRule, new_typeToId);
    }


    static GrammarRule findEquivalent(GrammarRule rule, ArrayList<GrammarRule> idToRule)
    {
        if(rule instanceof AltRule altRule && altRule.subRuleIds().size()==1)
        {
            int subId = altRule.subRuleIds().get(0);
            var subRule = idToRule.get(subId);
            if(subRule.datatype().equals(rule.datatype()))
                return findEquivalent(subRule, idToRule);
        }
        return rule;
    }

    static final Function<GrammarRule, List<Integer>> getSubIds =
        new SwitchOnType<GrammarRule, List<Integer>>()
            .on(AltRule.class, AltRule::subRuleIds)
            .on(ConcatRule.class, ConcatRule::subRuleIds)
            .on(RepeatRule.class, r->List.of(r.subRuleId()))
            .on(PeekRule.class, r->List.of(r.subRuleId()))
            .on(NegRule.class, r->List.of(r.subRuleId()))
            .on(RegexRule.class, r->List.of())
            .complete(GrammarRule.class);

    static final Function<GrammarRule,Function<Integer[],GrammarRule>> replaceIds =
        new SwitchOnType<GrammarRule,Function<Integer[],GrammarRule>>()
            .on(AltRule.class, r -> m -> new AltRule(
                m[r.id()], r.datatype(), replaceIdList(r.subRuleIds(), m))
            )
            .on(ConcatRule.class, r -> m -> new ConcatRule(
                m[r.id()], r.datatype(), replaceIdList(r.subRuleIds(), m), r.instantiator())
            )
            .on(RepeatRule.class, r -> m -> new RepeatRule(
                m[r.id()], r.datatype(), m[r.subRuleId()], r.min(), r.max())
            )
            .on(PeekRule.class, r -> m -> new PeekRule(
                m[r.id()], r.datatype(), m[r.subRuleId()])
            )
            .on(NegRule.class, r -> m -> new NegRule(
                m[r.id()], r.datatype(), m[r.subRuleId()])
            )
            .on(RegexRule.class, r -> m -> new RegexRule(
                m[r.id()], r.datatype(), r.regex(), r.instantiator())
            )
            .complete(GrammarRule.class);

    static List<Integer> replaceIdList(List<Integer> list, Integer[] idMap)
    {
        return list.stream().map(id->idMap[id]).toList();
    }




}
