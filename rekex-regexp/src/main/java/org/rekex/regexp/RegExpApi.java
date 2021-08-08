package org.rekex.regexp;

import org.rekex.regexp.RegExp.*;

import java.util.regex.Pattern;

/**
 * This class contains static factory methods to create and transform regular expressions,
 * using data structure of {@link RegExp}.
 * <p>
 *     For convenience, many methods, like {@link #seq(Object...)}, accept `Object` as arguments,
 *     which are automatically converted to `RegExp`.
 *     `Character` or `Integer` is converted to a single character.
 *     `String` is converted to a sequence of single characters.
 * </p>
 * <p>
 *     In addition, some methods, like {@link #intersect(Object...)}, expect arguments to be character classes;
 *     automatic conversion will be attempted, e.g. to convert `alt(cc1,cc2)` to `union(cc1,cc2)`,
 *     and `seq(cc1)` to `cc1`.
 * </p>
 * <p>
 *     For {@link #opt(Object...)}, {@link #rep0(Object...)}, {@link #rep1(Object...)},
 *     multiple arguments will be interpreted as a sequence,
 *     e.g. `opt(r1, r2)` is equivalent to `opt(seq(r1, r2))`.
 * </p>
 */
public class RegExpApi
{
    // char class ==============================================================

    // no need to have ch(char)=>Single. just directly use char/int in args.
    // if user must have a RegExp for a char, use alt('A').


    // ch("abc") = alt('a', 'b', 'c')
    // for convenience of enumerating a lot of chars; quite useful.

    /**
     * A character class that contains all code points in `chars`.
     */
    public static CharClass ch(String chars)
    {
        return union((Object[])stringToCc(chars));
    }

    /**
     * A character class with characters in [from-to], inclusive.
     */
    public static CharClass.Range range(int from, int to)
    {
        return new CharClass.Range(from, to);
    }

    /**
     * A predefined character class, e.g. `predefined("\\d")`.
     */
    public static CharClass.Predefined predefined(String regex)
    {
        return new CharClass.Predefined(regex);
    }

    /**
     * Union of character classes. Each `arg` must be convertible to a character class.
     */
    public static CharClass union(Object... args)
    {
        var args2 = arrayObjToCharClass(args); // throws
        if(args2.length==1) return args2[0];
        return new CharClass.Union(args2);
    }

    /**
     * Intersection of character classes. Each `arg` must be convertible to a character class.
     */
    public static CharClass intersect(Object... args)
    {
        var args2 = arrayObjToCharClass(args); // throws
        if(args2.length==1) return args2[0];
        return new CharClass.Intersection(args2);
    }

    /**
     * Negation of a character classes. The `arg` must be convertible to a character class.
     */
    // e.g. negate(alt('A', 'B'))
    // accept only one arg; otherwise negate('A','B') would be confusing.
    public static CharClass negate(Object arg)
    {
        var cc = objToCharClass(arg, "arg");
        return new CharClass.Negation(cc);
    }



    // alt, seq, quant =====================================================================

    /**
     * Concatenation of subexpressions.
     */
    public static RegExp seq(Object... args)
    {
        RegExp[] args2 = arrayObjToRegExp(args);
        if(args2.length==1) return args2[0];
        return new Concatenation(args2);
    }

    /**
     * Alternation of subexpressions.
     */
    public static RegExp alt(Object... args)
    {
        RegExp[] args2 = arrayObjToRegExp(args);
        if(args2.length==1) return args2[0];
        return new Alternation(args2);
    }


    // var-args in opt(), rep0(), rep1() are interpreted as sequence.
    //    quant(a, b) => quant(seq(a,b))
    //    quant(seq(...)) is quite common in grammar definitions

    /** Optional, i.e. X? */
    public static Quantified opt(Object... sequence)
    {
        return times(0, 1, seq(sequence));
    }

    // rep0() and rep1() - it's not obvious what rep() means; make it clear.

    /** Repeat 0 or more times, i.e. X* */
    public static Quantified rep0(Object... sequence)
    {
        return times(0, Quantified.INFINITY, seq(sequence));
    }

    /** Repeat 1 or more times, i.e. X+ */
    public static Quantified rep1(Object... sequence)
    {
        return times(1, Quantified.INFINITY, seq(sequence));
    }

    // times(n,exp), times(min, max, exp)
    // don't name the methods starting with "rep". rep0/rep1 meant 0-INF, 1-INF.
    // don't use (Object... args) for exp
    //    dangerous to interpret the 2nd param - is it max or an exp?
    //       could we use diff names for methods? name1(n, args..), name2(min, max, args..)
    //          very plausible that user misremembers, writes name1(min, max, args..) anyway.
    //    times() are not frequently used; it's ok to be restrictive. and it's unlikely that
    //       users need time(3, 'A'), times (3, "ABC"); usually the arg is already an RegExp.

    /** Repeat exactly `n` times */
    public static Quantified times(int n, RegExp exp)
    {
        return times(n, n, exp);
    }

    /** Repeat `min` to `max` times, inclusive. Set `max=Long.MAX_VALUE` as infinity. */
    public static Quantified times(int min, long max, RegExp exp)
    {
        return new Quantified(exp, min, max, Quantified.greedy);
    }

    /** Reluctant quantifier */
    public static Quantified reluctant(Quantified q)
    {
        return new Quantified(q.arg(), q.min(), q.max(), Quantified.reluctant);
    }
    /** Possessive quantifier */
    public static Quantified possessive(Quantified q)
    {
        return new Quantified(q.arg(), q.min(), q.max(), Quantified.possessive);
    }

    // we could, but we are not providing higher constructs like sepBy().
    // at that point, user probably needs a parser to capture parts.


    // others ==============================================================

    /** An un-named group */
    public static Group.Unnamed group(RegExp arg)
    {
        return new Group.Unnamed(arg);
    }
    /** A named group */
    public static Group.Named group(String name, RegExp arg)
    {
        return new Group.Named(arg, name);
    }

    /** An atomic group */
    public static AtomicGroup atomicGroup(RegExp arg)
    {
        return new AtomicGroup(arg);
    }

    /** Back reference with a group name */
    public static BackReference.WithName backRef(String name)
    {
        return new BackReference.WithName(name);
    }

    /** Back reference to a named group */
    public static BackReference.WithName backRef(Group.Named namedGroup)
    {
        return backRef(namedGroup.name());
    }

    /** Back reference with a group number */
    public static BackReference.WithNumber backRef(int number)
    {
        return new BackReference.WithNumber(number);
    }

    /** Turn flags on/off */
    public static Flagged flag(boolean on, int flags, RegExp arg)
    {
        // throws for unknown flag bits. user can always directly new Flagged() if needed.
        String flagStr = PkgUtil.flagBitsToStr(flags);
        return new Flagged(arg, on?flagStr:"", on?"":flagStr);
    }

    // for nested   (case-insensitive .... (case_sensitive.... ) )
    //    use flag(off, Pattern.CASE_INSENSITIVE) in inner scope.

    /** Turn on the flag CASE_INSENSITIVE */
    public static Flagged ignoreCase(RegExp arg)
    {
        return flag(true, Pattern.CASE_INSENSITIVE, arg);
    }

    // boundaries
    // use should define an alias before use, e.g. var EOF = boundary_end_of_input();

    /** A boundary matcher, e.g. `boundary("\\b{g}")`. */
    public static Boundary boundary(String regex)
    {
        return new Boundary(regex);
    }
    /** Same as `boundary("^")` */
    public static Boundary boundary_beginning_of_line()
    {
        return boundary("^");
    }
    /** Same as `boundary("$")` */
    public static Boundary boundary_end_of_line()
    {
        return boundary("$");
    }
    /** Same as `boundary("\\b")` */
    public static Boundary boundary_word_boundary()
    {
        return boundary("\\b");
    }
    /** Same as `boundary("\\B")` */
    public static Boundary boundary_non_word_boundary()
    {
        return boundary("\\B");
    }
    /** Same as `boundary("\\A")` */
    public static Boundary boundary_beginning_of_input()
    {
        return boundary("\\A");
    }
    /** Same as `boundary("\\G")` */
    public static Boundary boundary_end_of_previous_match()
    {
        return boundary("\\G");
    }
    /** Same as `boundary("\\Z")` */
    public static Boundary boundary_end_of_input_but_for_final_terminator()
    {
        return boundary("\\Z");
    }
    /** Same as `boundary("\\z")` */
    public static Boundary boundary_end_of_input()
    {
        return boundary("\\z");
    }

    /** A lookahead, positive or negative */
    public static Lookaround lookahead(boolean positive, RegExp arg)
    {
        return new Lookaround(arg, true, positive);
    }

    /** A lookbehind, positive or negative */
    public static Lookaround lookbehind(boolean positive, RegExp arg)
    {
        return new Lookaround(arg, false, positive);
    }


    /** An opaque regex */
    public static Opaque opaque(String regex)
    {
        return new Opaque(regex);
    }


    // ..........................................................................................

    // flatten nested seq, alt, union, intersect, neg; may convert alt to union.
    // don't expect it to be too smart, e.g.  alt(seq(A,B), B) => seq(opt(A), B)
    // we don't proactively simplify; it usually doesn't matter in toRegex()
    //    which naturally flattens same-precedence syntactic structures.
    // user decision: to simplify or not?
    // pros:
    //     may convert alt to union, producing shorter and nicer regex
    //     display a shallower tree in toTreeText()
    // cons:
    //     will lose the original tree structure, and node identities
    //     might be slow
    // so, it doesn't matter much; but user may choose to do it which doesn't hurt.

    /** Perform some simplifications to the regular expression */
    public static RegExp simplify(RegExp exp)
    {
        return ToSimplify.simplify(exp);
    }

    /** Return the typical string representation of the regular expression. */
    public static String toRegex(RegExp exp)
    {
        return ToRegex.regex(exp);
    }

    /** Return a multiline text that displays the tree structure of the regular expression. */
    public static String toTreeText(RegExp exp)
    {
        return ToTreeText.toTreeText(exp);
    }

    /** Find the group number of `group` in `exp`; return -1 if not found. */
    public static int findGroupNumberIn(Group group, RegExp exp)
    {
        // finding depends on object identity ==
        // if `group` exists in multiple nodes in the tree of `exp`, the first match is returned.
        // simplify() may change object identities; but it doesn't change group numbering.
        //     user should call findGroupNumberIn() before simplify()
        return ToCountGroup.findGroupNumberIn(group, exp);
    }



    // ..........................................................................................

    static RegExp[] arrayObjToRegExp(Object... args)
    {
        RegExp[] args2 = new RegExp[args.length];
        for(int i=0; i<args.length; i++)
            args2[i] = objToRegex(args[i]);
        return args2;
    }

    static RegExp objToRegex(Object arg)
    {
        if(arg==null)
            throw new NullPointerException();

        if(arg instanceof RegExp r)
            return r;

        if(arg instanceof Character c)
            return new CharClass.Single(c.charValue());

        if(arg instanceof Integer i)
            return new CharClass.Single(i.intValue());

        if(arg instanceof String s) 
            return seq((Object[])stringToCc(s));
        // no support for char[]

        throw new IllegalArgumentException("unexpected type: "+arg.getClass());
    }

    static CharClass.Single[] stringToCc(String string)
    {
        return string
            .codePoints()
            .mapToObj(CharClass.Single::new)
            .toArray(CharClass.Single[]::new);
    }






    static CharClass exceptImpl(RegExp exp, Object[] args)
    {
        CharClass thiz = objToCharClass(exp, "exp");
        if(args.length==0) // use case:  var cc = exp.except();
            return thiz;
        var neg = negate(union(args)); // throws
        return intersect(thiz, neg);

    }
    static CharClass objToCharClass(Object arg, String argDesc)
    {
        RegExp exp = objToRegex(arg);
        // could be alt() that's convertible to union(), or a seq(cc)
        if(!(exp instanceof CharClass))
            exp = ToSimplify.simplify(exp);
        if(exp instanceof CharClass cc)
            return cc;
        throw new IllegalArgumentException(argDesc+" is not a CharClass: "+arg);
    }
    static CharClass[] arrayObjToCharClass(Object... args)
    {
        CharClass[] args2 = new CharClass[args.length];
        for(int i=0; i<args.length; i++)
            args2[i] = objToCharClass(args[i], "args["+i+"]");
        return args2;
    }
}
