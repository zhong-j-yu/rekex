package org.rekex.helper.datatype;

import org.rekex.helper.datatype.seq.Seq2;
import org.rekex.spec.Ctor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * One or more T, separated by S.
 */
// ( T (S T)* )?
// the name `SepBy1` is originated from Parsec.
public record SepBy1<T, S>
    (List<T> values, List<S> separators)
    implements SepBy<T,S>
{

    public <R> R reduce(Function<T,R> f0, Function<R, Function<S, Function<T, R>>> fRest)
    {
        R r = f0.apply(values.get(0));
        for(int i=0; i<separators.size(); i++)
        {
            S sep = separators.get(i);
            T val = values.get(i+1);
            r = fRest.apply(r).apply(sep).apply(val);
        }
        return r;
    }

    public <R> SepBy1<R, S> map(Function<T, R> func)
    {
        var valuesR = values().stream().map(func).toList();
        return new SepBy1<>(valuesR, separators);
    }

    public T reduce(Function<T, Function<S, Function<T, T>>> f)
    {
        T t = values.get(0);
        for(int i=0; i<separators.size(); i++)
        {
            S sep = separators.get(i);
            T val = values.get(i+1);
            t = f.apply(t).apply(sep).apply(val);
        }
        return t;
    }

    @Ctor
    public static <T,S> SepBy1<T,S> ctor(T first, List<Seq2<S,T>> rest)
    {
        ArrayList<T> values = new ArrayList<>(1+rest.size());
        ArrayList<S> separators = new ArrayList<>(rest.size());
        values.add(first);
        for(var st : rest)
        {
            separators.add(st.v1());
            values.add(st.v2());
        }
        return new SepBy1<>(values, separators);
    }

}
