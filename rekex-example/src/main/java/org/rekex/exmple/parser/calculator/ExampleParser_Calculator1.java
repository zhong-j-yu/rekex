package org.rekex.exmple.parser.calculator;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.StrWs;
import org.rekex.helper.datatype.SepBy1;
import org.rekex.parser.PegParser;
import org.rekex.spec.Regex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

// separate tokenizer
// whitespaces: optional Space/Tab between tokens
// eval logics are defined in place.
public interface ExampleParser_Calculator1
{
    // whitespaces ----------------------------------------------------

    String wsChars = " \t";

    // equivalent to @StrWs, with default whitespace chars
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface Word
    {
        String[] value();
        AnnoMacro<Word, StrWs> toStrWs = StrWs.Macro.of(Word::value, wsChars);
    }

    // zero or more whitespaces
    enum OptWs{ @Word("")I }

    // tokens --------------------------------------------------------

    enum TermOp {
        @Word("+") plus,
        @Word("-") minus;

        public int eval(int x, int y){
            return this==plus ? x+y : x-y;
        }
    }
    enum FactorOp {
        @Word({"/","÷"}) div,
        @Word({"*","×"}) mul,
        @Word("") mul_implicit; // fac1 fac2 == fac1 * fac2

        public int eval(int x, int y){
            return this==div ? x/y : x*y;
        }
    }

    enum PL { @Word("(")I }
    enum PR { @Word(")")I }

    // one or more digits; skip trailing whitespaces.
    record Digits(@Regex("[0-9]+")String str, OptWs trailingWs){
        public int toInt(){ return Integer.parseInt(str); }
    }

    record Input(OptWs leadingWs, Expr expr){}

    // datatypes -----------------------------------------------------

    record Expr(SepBy1<Term, TermOp> term_ops)
    {
        public int eval() {
            return term_ops()
                .map(Term::eval)
                .reduce(x->op->y->op.eval(x,y));
        }
    }

    record Term(SepBy1<Factor, FactorOp> fac_ops)
    {
        public int eval() {
            return fac_ops()
                .map(Factor::eval)
                .reduce(x->op->y->op.eval(x,y));
        }
    }

    sealed interface Factor
    {
        public int eval();
    }

    record Parens(PL lp, Expr expr, PR rp) implements Factor
    {
        public int eval() { return expr.eval(); }
    }

    record Num(Digits digits) implements Factor
    {
        public int eval() { return digits.toInt(); }
    }

    // test -----------------------------------------------------

    public static PegParser<Input> parser()
    {
        return PegParser.of(Input.class);
    }
    public static Function<Input,Integer> eval()
    {
        return (input)->input.expr.eval();
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator1", parser(), eval());
    }

}
