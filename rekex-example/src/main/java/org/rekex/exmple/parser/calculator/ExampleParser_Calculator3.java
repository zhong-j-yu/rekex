package org.rekex.exmple.parser.calculator;

import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.spec.Regex;

import java.util.function.Function;

public interface ExampleParser_Calculator3
{
    // all ctors are defined in the catalog
    //
    // the AST to be produced is just the value of the expression,
    // so we only need to store numeric values in tree nodes.
    // but we still need different datatypes to represent different symbols

    record Exp0(int val){}
    record Exp1(int val){}
    record Exp2(int val){}


    class CtorCatalog
    {
        public Exp0 exp0(SepBy1<Exp1, @Ch("+-")String> items)
        {
            int val = items.map(Exp1::val).reduce(x->op->y->eval(x, op, y));
            return new Exp0(val);
        }

        public Exp1 exp1(SepBy1<Exp2, @Ch("*/")String> items)
        {
            int val = items.map(Exp2::val).reduce(x->op->y->eval(x, op, y));
            return new Exp1(val);
        }

        public Exp2 exp2_parens(@Ch("(")Void lp, Exp0 expr, @Ch(")")Void rp)
        {
            return new Exp2(expr.val);
        }
        public Exp2 exp2_num(@Regex("[0-9]+") String str)
        {
            return new Exp2(Integer.parseInt(str));
        }

        static int eval(int x, String op, int y)
        {
            return ExampleParser_Calculator0.calc(x, op, y);
        }
    }


    // test -----------------------------------------------------

    public static PegParser<Exp0> parser()
    {
        return PegParser.of(Exp0.class, CtorCatalog.class, new CtorCatalog());
    }
    public static Function<Exp0,Integer> eval()
    {
        return expr->expr.val;
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator3", parser(), eval());
    }

}
