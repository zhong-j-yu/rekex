package org.rekex.exmple.parser.calculator;

import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.spec.Regex;

import java.util.function.Function;

public interface ExampleParser_Calculator0
{
    // datatypes -----------------------------------------------------

    // terms separated by + or -
    record Expr(SepBy1<Term, @Ch("+-")String> term_ops){}

    // factors separated by * or /
    record Term(SepBy1<Factor, @Ch("*/")String> fac_ops){}

    sealed interface Factor permits Parens, Num{}

    // ( expr )
    record Parens(@Ch("(")Void lp, Expr expr, @Ch(")")Void rp)
        implements Factor{}

    // unsigned integer
    record Num(@Regex("[0-9]+") String str)
        implements Factor {}


    // eval -----------------------------------------------------

    static int eval(int x, String op, int y)
    {
        return switch (op){
            case "+" -> x+y;
            case "-" -> x-y;
            case "*" -> x*y;
            case "/" -> x/y;
            default  -> throw new AssertionError();
        };
    }

    static int eval(Expr expr)
    {
        return expr.term_ops()
            .map(term->eval(term))
            .reduce(x->op->y-> eval(x, op, y));
    }

    static int eval(Term term)
    {
        return term.fac_ops()
            .map(fac->eval(fac))
            .reduce(x->op->y-> eval(x, op, y));
    }

    static int eval(Factor factor)
    {
        if(factor instanceof Parens parens)
            return eval(parens.expr());
        if(factor instanceof Num num)
            return eval(num);
        throw new AssertionError();
    }

    static int eval(Num num)
    {
        return Integer.parseInt(num.str);
    }

    // test -----------------------------------------------------

    public static PegParser<Expr> parser()
    {
        return PegParser.of(Expr.class);
    }
    public static Function<Expr,Integer> eval()
    {
        return expr->eval(expr);
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator0", parser(), eval());
    }

}
