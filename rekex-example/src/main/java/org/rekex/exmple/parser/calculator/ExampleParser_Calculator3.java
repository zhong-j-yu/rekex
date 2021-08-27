package org.rekex.exmple.parser.calculator;

import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserBuilder;
import org.rekex.spec.Regex;

import java.util.function.Function;

// a rule catalog for syntax rules.
// produces AST nodes
public interface ExampleParser_Calculator3
{
    // AST nodes; without any reference to concrete syntax.
    // thinning aggressively, storing only the int value

    record Expr(int val){}
    record Term(int val){}
    record Factor(int val){}

    class RulesCatalog
    {
        public Expr expr(SepBy1<Term, @Ch("+-")String> term_ops)
        {
            int val = term_ops.map(Term::val)
                .reduce(x->op->y->eval(x, op, y));
            return new Expr(val);
        }

        public Term term(SepBy1<Factor, @Ch("*/")String> fac_ops)
        {
            int val = fac_ops.map(Factor::val)
                .reduce(x->op->y->eval(x, op, y));
            return new Term(val);
        }

        public Factor parens(@Ch("(")char lp, Expr expr, @Ch(")")char rp)
        {
            return new Factor(expr.val);
        }

        public Factor num(@Regex("[0-9]+") String str)
        {
            return new Factor(Integer.parseInt(str));
        }

        static int eval(int x, String op, int y)
        {
            return ExampleParser_Calculator0.eval(x, op, y);
        }
    }


    // test -----------------------------------------------------

    public static PegParser<Expr> parser()
    {
        return new PegParserBuilder()
            .rootType(Expr.class)
            .catalogClass(RulesCatalog.class)
            .build(new RulesCatalog());
    }
    public static Function<Expr,Integer> eval()
    {
        return expr->expr.val;
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator3", parser(), eval());
    }

}
