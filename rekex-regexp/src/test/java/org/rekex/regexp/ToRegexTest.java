package org.rekex.regexp;

import org.junit.jupiter.api.Test;
import org.rekex.regexp.RegExp.Alternation;
import org.rekex.regexp.RegExp.CharClass;
import org.rekex.regexp.RegExp.CharClass.Intersection;
import org.rekex.regexp.RegExp.CharClass.Union;
import org.rekex.regexp.RegExp.Concatenation;

import java.util.function.Consumer;

import static org.rekex.regexp.RegExpApi.*;

class ToRegexTest extends RegExpTestBase
{
    @Test
    void testPrecedence()
    {
        // no parens for atomics
        var A = alt('A');
        RegExp[] atomics = {
            A,
            negate('B'),
            predefined("C"),
            new Alternation(),   // alt() is atomic
            new Alternation(alt('D')),      // alt(X) ~ X
            new Concatenation(alt('E')),    // seq(X) ~ X
            boundary_beginning_of_input(),
            lookahead(true, alt('F')),
            group(alt('G')),
            group("g1", alt('H')),
            backRef(1),
            //backRef("g1"),
            atomicGroup(alt('I')),
            ignoreCase(alt('J'))
        };
        for(var a : atomics)
            test(opt(a)).regex(toRegex(a)+"?");
        test(seq((Object[])atomics))
            .regex(join("", atomics));
        test(alt((Object[])atomics))
            .regex(join("|", atomics));

        var quant = opt(A);
        // quant(quant()) requires parens
        test(opt(quant))
            .regex("(?:A?)?");
        test(seq(A, quant))
            .regex("AA?");
        test(alt(A, quant))
            .regex("A|A?");

        var seq0 = seq();  // requires parens in quant()
        test(opt(seq0))
            .regex("(?:)?");
        test(seq(A, seq0))
            .regex("A");
        test(alt(A, seq0))
            .regex("A|");

        Consumer<RegExp> t_seq2 = seq2->{
            test(opt(seq2))
                .regex("(?:AA)?");
            test(seq(A, seq2))
                .regex("AAA");
            test(alt(A, seq2))
                .regex("A|AA");
        };
        var seq2 = seq(A, A);
        t_seq2.accept(seq2);

        Consumer<RegExp> t_alt2 = alt2->{
            test(opt(alt2))
                .regex("(?:B|C)?");
            test(seq(A, alt2))
                .regex("A(?:B|C)");
            test(alt(A, alt2))
                .regex("A|B|C");
        };
        var alt2 = alt("B", "C");
        t_alt2.accept(alt2);

        // seq(X) ~ X
        t_alt2.accept(new Concatenation(alt2));
        t_seq2.accept(new Concatenation(seq2));
        // alt(X) ~ X
        t_alt2.accept(new Alternation(alt2));
        t_seq2.accept(new Alternation(seq2));


        // regex ~ alt ~ opaque
        var opaque = opaque("X");
        test(opt(opaque))
            .regex("(?:X)?");
        test(seq(A, opaque))
            .regex("A(?:X)");
        test(alt(A, opaque))
            .regex("A|X");

        // char classes

        Consumer<CharClass> t_range = range->{
            test(range)
                .regex("[0-9]");
            test(union(A, range))
                .regex("[A0-9]");
            test(intersect(A, range))
                .regex("[A&&0-9]");
        };
        var range = range('0', '9');
        t_range.accept(range);

        var union0 = union();
        test(union0)
            .regex("[0&&1]");
        test(union(A, union0))
            .regex("[A[0&&1]]");
        test(intersect(A, union0))
            .regex("[A&&0&&1]");

        var intersect0 = intersect();
        test(intersect0)
            .regex("[\\u0000-\\x{10FFFF}]");
        test(union(A, intersect0))
            .regex("[A\\u0000-\\x{10FFFF}]");
        test(intersect(A, intersect0))
            .regex("[A&&\\u0000-\\x{10FFFF}]");


        Consumer<CharClass> t_union2 = union2->{
            test(union2)
                .regex("[BC]");
            test(union(A, union2))
                .regex("[ABC]");
            test(intersect(A, union2))
                .regex("[A&&BC]");
        };
        var union2 = union(('B'), ('C'));
        t_union2.accept(union2);

        Consumer<CharClass> t_intersect2 = intersect2->{
            test(intersect2)
                .regex("[B&&C]");
            test(union(A, intersect2))
                .regex("[A[B&&C]]");
            test(intersect(A, intersect2))
                .regex("[A&&B&&C]");
        };
        var intersect2 = intersect(('B'), ('C'));
        t_intersect2.accept(intersect2);

        // union(X) ~ X
        t_range.accept(new Union(range));
        t_union2.accept(new Union(union2));
        t_intersect2.accept(new Union(intersect2));
        // intersect(X) ~ X
        t_range.accept(new Intersection(range));
        t_union2.accept(new Intersection(union2));
        t_intersect2.accept(new Intersection(intersect2));

    }


    static String join(String op, RegExp[] args)
    {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<args.length; i++)
        {
            if(i>0) sb.append(op);
            sb.append(toRegex(args[i]));
        }
        return sb.toString();
    }

}