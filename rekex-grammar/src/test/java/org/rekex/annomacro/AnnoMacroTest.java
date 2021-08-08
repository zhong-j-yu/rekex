package org.rekex.annomacro;

import org.junit.jupiter.api.Test;
import org.rekex.common_util.AnnoBuilder;

import static org.junit.jupiter.api.Assertions.*;

class AnnoMacroTest
{
    static final AnnoMacro.Resolver resolver = new AnnoMacro.Resolver();

    @interface Value
    {
        int value();
    }
    @interface Multiply
    {
        int x();
        int y();
        AnnoMacro<Multiply, Value> macro = thiz->
            AnnoBuilder.build(Value.class, thiz.x()*thiz.y());
    }
    // f(0)->v(1); f(n)=>mul(n, f(n-1))
    @interface Factorial
    {
        int value();
        AnnoMacro<Factorial, Object> macro = thiz->{
            int n = thiz.value();
            if(n<0)
                throw new IllegalArgumentException();
            if(n==0)
                return AnnoBuilder.build(Value.class, 1);
            var f_n_minus_1 = AnnoBuilder.build(Factorial.class, n-1);
            Value v_n_minus_1 = resolver.resolve(f_n_minus_1);
            return AnnoBuilder.build(Multiply.class,
                Multiply::x, n,
                Multiply::y, v_n_minus_1.value());
        };
    }

    @Test
    void  testFactorial()
    {
        int v=1;
        for(int n=0; n<=4; n++)
        {
            if(n>0) v*=n;
            var f_n = AnnoBuilder.build(Factorial.class, n);
            Value v_n = resolver.resolve(f_n);
            assertEquals(v, v_n.value());
        }
    }
}