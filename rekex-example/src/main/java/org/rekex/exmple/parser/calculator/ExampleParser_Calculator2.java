package org.rekex.exmple.parser.calculator;

import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.spec.Ctor;
import org.rekex.spec.Regex;

import java.util.function.Function;

public interface ExampleParser_Calculator2
{
    // aggressively thinning tree nodes
    //
    // canonical constructors only store numeric values;
    // explicit ctors are declared to express grammar rules

    // datatypes -----------------------------------------------------

    record Exp0(int val)
    {
        @Ctor public Exp0(SepBy1<Exp1, @Ch("+-")String> term_ops)
        {
            this(term_ops.map(Exp1::val).reduce(x->op->y->calc(x, op, y)));
        }
    }


    record Exp1(int val)
    {
        @Ctor public Exp1(SepBy1<Exp2, @Ch("*/")String> fac_ops)
        {
            this(fac_ops.map(Exp2::val).reduce(x->op->y->calc(x, op, y)));
        }
    }

    record Exp2(int val)
    {
        // we don't need two subtypes for Exp2; just define two ctors.

        @Ctor public static
        Exp2 parens(@Ch("(")Void lp, Exp0 expr, @Ch(")")Void rp)
        {
            return new Exp2(expr.val);
        }

        @Ctor public static
        Exp2 num(@Regex("[0-9]+") String str)
        {
            return new Exp2(Integer.parseInt(str));
        }
    }

    static int calc(int x, String op, int y)
    {
        return ExampleParser_Calculator0.calc(x, op, y);
    }

    // test -----------------------------------------------------

    public static PegParser<Exp0> parser()
    {
        return PegParser.of(Exp0.class);
    }
    public static Function<Exp0,Integer> eval()
    {
        return expr->expr.val;
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator2", parser(), eval());
    }

}
