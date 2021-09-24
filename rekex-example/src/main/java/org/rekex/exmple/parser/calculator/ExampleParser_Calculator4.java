package org.rekex.exmple.parser.calculator;

import org.rekex.annotype.AnnoType;
import org.rekex.annotype.PrimitiveType;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.exmple.parser.ExampleParserUtil;
import org.rekex.helper.anno.Ch;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserBuilder;
import org.rekex.spec.Regex;

import java.lang.annotation.*;
import java.util.Set;
import java.util.function.Function;

public interface ExampleParser_Calculator4
{
    // grammar, designed in a way that every symbol can be mirrored by a primitive value.
    //
    // expr  -> term termR
    // termR -> +- term termR | ""
    // term  -> fact factR
    // factR -> */ fact factR | ""
    // fact  -> ( expr ) | digits
    // digits-> digit digitR
    // digitR-> digit digitR  | ""
    // digit -> 0-9


    // use annotated primitive types to represent grammar symbols, e.g. @N("expr")double
    @Target(ElementType.TYPE_USE)@Retention(RetentionPolicy.RUNTIME)
    @interface N{ String value(); }

    // It is going to be painfully obvious that N.value always agrees
    // with the parameter name or the method name, thus redundant.
    // We could derive a grammar from identifiers only, e.g.
    //     double term(double factor);  double factor(...);
    // which represents rules:  term->factor, factor->...
    // That is beautiful; could be another useful project.
    //
    // However, we need @N, in this example, only because we use 1 primitive type
    // to represent different things. Most grammars probably require
    // distinct Types for different things, thus don't need annotations anyway.
    //
    // the point of this example, besides inducing double visions,
    // is to examine the *possibility*, not to encourage.

    class CtorCatalog
    {

        public @N("expr")double expr(@N("term")double term, @N("termR")double termR)
        {
            return termR+term;
        }

        public @N("termR")double termR_1(@Ch("+-")char op, @N("term")double term, @N("termR")double termR)
        {
            return op=='+' ? termR+term : termR-term; // // A-B+C=C-B+A
        }
        public @N("termR")double termR_2()
        {
            return 0;
        }

        public @N("term")double term(@N("fact")double fact, @N("factR")double factR)
        {
            return factR*fact;
        }


        public @N("factR")double factR_1(@Ch("*/")char op, @N("fact")double fact, @N("factR")double factR)
        {
            return op=='*' ? factR*fact : factR/fact; // A/B*C=C/B*A
            // it doesn't work for integer divisions. (not precise for doubles either)
        }
        public @N("factR")double factR_2()
        {
            return 1;
        }

        public @N("fact")double fact_1(@Ch("(")Void lp, @N("expr")double expr, @Ch(")")Void rp)
        {
            return expr;
        }

        public @N("fact")double fact_2(@N("digits")Digits digits)
        {
            return digits.value;
        }

        public record Digits(int count, int value){}
        // it can be replaced by `long`, so that only primitives are referenced by ctors.
        // keep this class for now to see more clearly how digits are concatenated.

        public @N("digits")Digits digits(@N("digit")int digit, @N("digitR")Digits digitR)
        {
            return concat(digit, digitR);
        }

        public @N("digitR")Digits digitR_1(@N("digit")int digit, @N("digitR")Digits digitR)
        {
            // this is the same as "digits"; but we saved one indirection.
            return concat(digit, digitR);
        }
        public @N("digitR")Digits digitR_2()
        {
            return new Digits(0,0);
        }

        public @N("digit")int digit(@Regex("[0-9]")char c)
        {
            return c-'0';
        }

        private static Digits concat(int digit, Digits digitR)
        {
            long value = digit;
            for(int i = 0; i<digitR.count; i++)
                value *= 10;
            value += digitR.value;
            if(value>Integer.MAX_VALUE)
                throw new NumberFormatException("number too big");
            return new Digits(digitR.count +1, (int)value);
        }

    }


    // test -----------------------------------------------------

    public static PegParser<Double> parser()
    {
        // @N("expr")double
        Annotation n_expr = AnnoBuilder.build(N.class, "expr");
        AnnoType type = new PrimitiveType(Set.of(n_expr), double.class);

        return new PegParserBuilder()
            .rootType(type)
            .catalogClass(CtorCatalog.class)
            .build(new CtorCatalog());
    }
    public static Function<Double, Double> eval()
    {
        return d->d;
    }

    public static void main(String[] args) throws Exception
    {
        ExampleParserUtil.testInputs("Calculator4", parser(), eval());
    }

}
