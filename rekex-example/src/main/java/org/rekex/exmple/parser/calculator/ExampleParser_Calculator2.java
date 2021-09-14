package org.rekex.exmple.parser.calculator;

import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.spec.Ctor;
import org.rekex.spec.Regex;

import java.util.function.Function;

// aggressively thinning parse tree nodes, storing only the int value
public interface ExampleParser_Calculator2
{
    // datatypes -----------------------------------------------------

    record Expr(int val){
        @Ctor // terms separated by + or -
        public Expr(SepBy1<Term, @Ch("+-")String> term_ops){
            this(term_ops.map(Term::val)
                .reduce(x->op->y->eval(x, op, y)));
        }
    }


    record Term(int val){
        @Ctor // factors separated by * or /
        public Term(SepBy1<Factor, @Ch("*/")String> fac_ops){
            this(fac_ops.map(Factor::val)
                .reduce(x->op->y->eval(x, op, y)));
        }
    }

    record Factor(int val){
        @Ctor // ( expr )
        public static Factor parens(@Ch("(")Void lp, Expr expr, @Ch(")")Void rp){
            return new Factor(expr.val);
        }
        @Ctor // unsigned integer
        public static Factor num(@Regex("[0-9]+") String str){
            return new Factor(Integer.parseInt(str));
        }
    }

    static int eval(int x, String op, int y)
    {
        return ExampleParser_Calculator0.eval(x, op, y);
    }

    // test -----------------------------------------------------

    public static PegParser<Expr> parser()
    {
        return PegParser.of(Expr.class);
    }
    public static Function<Expr,Integer> eval()
    {
        return expr->expr.val;
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator2", parser(), eval());
    }

}
