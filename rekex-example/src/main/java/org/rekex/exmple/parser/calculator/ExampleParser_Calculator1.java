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

public interface ExampleParser_Calculator1
{
    // in this example:
    //   - whitespaces: optional Space/Tab between tokens
    //   - token datatypes are extracted
    //   - eval() defined inside datatypes


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

    record Input(OptWs leadingWs, Exp0 e0){}

    // tokens --------------------------------------------------------

    enum Op1
    {
        @Word("+") plus,
        @Word("-") minus;

        public int eval(int x, int y){
            return this==plus ? x+y : x-y;
        }
    }
    enum Op2
    {
        @Word({"/","÷"}) div,
        @Word({"*","×"}) mul,
        @Word("") mul_implicit; // a b == a * b

        public int eval(int x, int y){
            return this==div ? x/y : x*y;
        }
    }

    enum PL { @Word("(")I }
    enum PR { @Word(")")I }

    record Digits(@Regex("[0-9]+")String str, OptWs trailingWs){
        public int toInt(){ return Integer.parseInt(str); }
    }

    // composite datatypes -----------------------------------------------------

    record Exp0(SepBy1<Exp1, Op1> items)
    {
        public int eval() {
            return items().map(Exp1::eval).reduce(x->op->y->op.eval(x,y));
        }
    }

    record Exp1(SepBy1<Exp2, Op2> items)
    {
        public int eval() {
            return items().map(Exp2::eval).reduce(x->op->y->op.eval(x,y));
        }
    }

    sealed interface Exp2 permits Parens, Num
    {
        public int eval();
    }

    record Parens(PL lp, Exp0 expr, PR rp) implements Exp2
    {
        public int eval() { return expr.eval(); }
    }

    record Num(Digits digits) implements Exp2
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
        return (input)->input.e0.eval();
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator1", parser(), eval());
    }

}
