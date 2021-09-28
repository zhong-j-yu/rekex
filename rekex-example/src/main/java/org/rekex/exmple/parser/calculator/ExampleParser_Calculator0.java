package org.rekex.exmple.parser.calculator;

import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.spec.Regex;

import java.util.function.Function;

public interface ExampleParser_Calculator0
{
    // simple arithmetic expressions
    //
    //    E0 = E1 ( ("+"|"-") E1) *
    //    E1 = E2 ( ("*"|"/") E2) *
    //    E2 = "(" E0 ")" | Num
    //
    //  this is in a class of problems of "operator precedence",
    //  which can be defined in layers
    //
    //    E_(i-1) = Ei ( Oi Ei )*

    // In this example, we simply express the grammar in algebraic datatypes;
    // the parser produces a parse tree,
    // which is then transformed to its numeric value

    // grammar as datatypes -----------------------------------------------------

    record Exp0(SepBy1<Exp1, @Ch("+-")String> items){}

    record Exp1(SepBy1<Exp2, @Ch("*/")String> items){}

    sealed interface Exp2 permits Parens, Num{}

    record Parens(@Ch("(")Void lp, Exp0 expr, @Ch(")")Void rp) implements Exp2 {}

    record Num(@Regex("[0-9]+")String str) implements Exp2 {}



    // eval; transform parse tree to AST; one function for each node type. ---------

    static int eval0(Exp0 e0)
    {
        return e0.items().map(e1->eval1(e1)).reduce(x->op->y->calc(x, op, y));
    }

    static int eval1(Exp1 e1)
    {
        return e1.items().map(e2->eval2(e2)).reduce(x->op->y->calc(x, op, y));
    }

    static int eval2(Exp2 e2)
    {
        if(e2 instanceof Parens parens)
            return eval0(parens.expr());
        if(e2 instanceof Num num)
            return Integer.parseInt(num.str);
        throw new AssertionError();
    }

    static int calc(int x, String op, int y)
    {
        return switch (op){
            case "+" -> x+y;
            case "-" -> x-y;
            case "*" -> x*y;
            case "/" -> x/y;
            default  -> throw new AssertionError();
        };
    }

    // test -----------------------------------------------------

    public static PegParser<Exp0> parser()
    {
        return PegParser.of(Exp0.class);
    }
    public static Function<Exp0,Integer> eval()
    {
        return expr-> eval0(expr);
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.genJava(ExampleParser_Calculator0.class, Exp0.class, null);
        
        ExampleParserUtil.testInputs("Calculator0", parser(), eval());
    }

}
