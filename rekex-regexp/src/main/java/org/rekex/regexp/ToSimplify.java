package org.rekex.regexp;

import org.rekex.common_util.SwitchOnType;
import org.rekex.regexp.RegExp.*;
import org.rekex.regexp.RegExp.CharClass.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

record ToSimplify()
{
    public static RegExp simplify(RegExp exp)
    {
        return simplify.apply(exp);
    }

    static final Function<RegExp, RegExp> simplify =
        new SwitchOnType<RegExp, RegExp>()
            .on(Single.class, ToSimplify::onSingle)
            .on(Range.class, ToSimplify::onRange)
            .on(Union.class, ToSimplify::onUnion)
            .on(Intersection.class, ToSimplify::onIntersection)
            .on(Negation.class, ToSimplify::onNegation)
            .on(Predefined.class, ToSimplify::onPredefined)
            .on(Alternation.class, ToSimplify::onAlternation)
            .on(Concatenation.class, ToSimplify::onConcatenation)
            .on(Quantified.class, ToSimplify::onQuantified)
            .on(Boundary.class, ToSimplify::onBoundary)
            .on(Lookaround.class, ToSimplify::onLookaround)
            .on(BackReference.WithNumber.class, ToSimplify::onBackReferenceWithNumber)
            .on(BackReference.WithName.class, ToSimplify::onBackReferenceWithName)
            .on(Group.Unnamed.class, ToSimplify::onGroupUnnamed)
            .on(Group.Named.class, ToSimplify::onGroupNamed)
            .on(AtomicGroup.class, ToSimplify::onAtomicGroup)
            .on(Flagged.class, ToSimplify::onFlagged)
            .on(Opaque.class, ToSimplify::onOpaque)
            .complete(RegExp.class);


    static Single onSingle(Single r)
    {
        return r;
    }

    static Range onRange(Range r)
    {
        return r;
    }

    static CharClass onUnion(Union r)
    {
        var args = flatten(r.args(), a ->
            (a instanceof Union union) ? union.args() : null);

        if(args.length==1)
            return args[0];

        return new Union(args);
    }

    static CharClass onIntersection(Intersection r)
    {
        var args = flatten(r.args(), a ->
            (a instanceof Intersection intersect) ? intersect.args() : null);

        if(args.length==1)
            return args[0];

        return new Intersection(args);
    }

    static CharClass onNegation(Negation r)
    {
        var cc = (CharClass) simplify(r.arg());
        if(cc instanceof Negation neg)
            return neg.arg();
        return new Negation(cc);
    }

    static Predefined onPredefined(Predefined r)
    {
        return r;
    }

    static RegExp onAlternation(Alternation r)
    {
        var args = flatten(r.args(), a ->
            (a instanceof Alternation alt) ? alt.args() : null);

        // grammars usually use Alt to union char classes; e.g. alt(SP, TAB)
        // we'll try to produce CharClass if possible.
        args = mergeAltCc(args);

        if(args.length==1)
            return args[0];

        return new Alternation(args);
    }

    static RegExp onConcatenation(Concatenation r)
    {
        var args = flatten(r.args(), a ->
            (a instanceof Concatenation seq) ? seq.args() : null);

        if(args.length==1)
            return args[0];

        return new Concatenation(args);
    }

    static Quantified onQuantified(Quantified q)
    {
        final var greedy = Quantified.greedy;
        var exp = simplify(q.arg());

        if(exp instanceof Quantified q1
            && q.greediness()==greedy && q1.greediness()==greedy)
        {
            // opt(rep1(e)) is not uncommon, try to flatten it to rep0(e).
            // X{2,3}{2,3} => X{4,9}
            try
            {
                long min2 = Math.multiplyExact(q.min(), q1.min());

                final var INF = Quantified.INFINITY;
                long max2 = (q.max()==0 || q1.max()==0) ? 0
                    : (q.max()==INF || q1.max()==INF) ? INF
                    : Math.multiplyExact(q.max(), q1.max());

                return new Quantified(q1.arg(), min2, max2, greedy);
            }
            catch (ArithmeticException e) // multiply overflow. really?
            {
                ; // that's ok. don't flatten then.
            }
        }

        return new Quantified(exp, q.min(), q.max(), q.greediness());
    }

    static Boundary onBoundary(Boundary r)
    {
        return r;
    }

    static Lookaround onLookaround(Lookaround r)
    {
        var exp = simplify(r.arg());
        return new Lookaround(exp, r.ahead(), r.positive());
    }

    static BackReference.WithNumber onBackReferenceWithNumber(BackReference.WithNumber r)
    {
        return r;
    }

    static BackReference.WithName onBackReferenceWithName(BackReference.WithName r)
    {
        return r;
    }

    static Group.Unnamed onGroupUnnamed(Group.Unnamed r)
    {
        var exp = simplify(r.arg());
        return new Group.Unnamed(exp);
    }

    static Group.Named onGroupNamed(Group.Named r)
    {
        var exp = simplify(r.arg());
        return new Group.Named(exp, r.name());
    }

    static AtomicGroup onAtomicGroup(AtomicGroup r)
    {
        var exp = simplify(r.arg());
        return new AtomicGroup(exp);
    }

    static Flagged onFlagged(Flagged r)
    {
        var exp = simplify(r.arg());
        return new Flagged(exp, r.onFlags(), r.offFlags());
    }

    static Opaque onOpaque(Opaque r)
    {
        return r;
    }











    // remove nested Seq Alt etc
    static <E extends RegExp> E[] flatten(E[] arr, Function<E,E[]> mapper)
    {
        {
            E[] arr1 = Arrays.copyOf(arr, arr.length);
            for(int i=0; i<arr.length; i++)
            {
                // safe cast. E is RegExp or CharClass
                @SuppressWarnings("unchecked")
                var sim = (E) simplify(arr[i]);
                arr1[i] = sim;
            }
            arr = arr1;
        }
        ArrayList<E> list = new ArrayList<>();
        for(E e : arr)
        {
            E[] a2 = mapper.apply(e);
            if(a2==null)
                list.add(e);
            else
                Collections.addAll(list, a2);
        }
        @SuppressWarnings("unchecked")
        E[] arr2 = (E[]) Array.newInstance(arr.getClass().getComponentType(), list.size());
        return list.toArray(arr2);
    }

    // merge adjacent char classes in Alt
    static RegExp[] mergeAltCc(RegExp[] args)
    {
        ArrayList<RegExp> list = new ArrayList<>();
        CharClass lastAsCC = null;

        for(var arg : args)
        {
            if(arg instanceof CharClass argCC)
            {
                if(argCC instanceof Union union && union.args().length==0)
                    continue; // empty union

                if(lastAsCC==null)
                {
                    list.add(arg);
                    lastAsCC = argCC;
                }
                else
                {
                    lastAsCC = new Union(lastAsCC, argCC);
                    lastAsCC = (CharClass) simplify(lastAsCC);
                    list.set(list.size()-1, lastAsCC);
                }
            }
            else
            {
                list.add(arg);
                lastAsCC = null;
            }
        }
        if(list.isEmpty())
            list.add(new Union());  // alt() => union()
        return list.toArray(new RegExp[list.size()]);
    }

}
