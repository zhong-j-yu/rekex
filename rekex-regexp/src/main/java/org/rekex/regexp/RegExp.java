package org.rekex.regexp;

import java.util.regex.Pattern;

// users shouldn't usually care about these datatypes. they use factory methods in RegExpApi.
//
// do not do flattening or other simplifications in these classes;
// allow any valid form of trees.
//
// don't provide "fluent" methods here (except the except() method)
// use prefix construction methods in RegExpApi,
// which more clearly shows the structure of what's being built.
//
// keep RegExp a pure algebraic data type, just to model the structure;
// refrain from adding any "oop" methods here.

/**
 * Data structure representing regular expressions, conforming to {@link java.util.regex.Pattern}.
 */
public sealed interface RegExp
{
    // no clone(), even though some of our code depends on ==,
    // and some nodes could be shared. user can enclose node creation
    // in a method; call the method multiple times to get a new node instance.

    // except() is defined on RegExp instead of on CharClass, because we don't promote
    // the concept of CharClass to users. Occasionally, except() is needed, we'll do
    // runtime type checking that all parties are CharClass, possibly after conversion.
    // tip: to check or convert an exp to CharClass, do var cc = exp.except();

    /**
     * `exp.except(args)` is equivalent to `intersect(exp, unions(args))`.
     *  `exp` and `arg` in `args` must all be character classes, or convertible to character classes.
     */
    default CharClass except(Object... args)
    {
        return RegExpApi.exceptImpl(this, args);
    }


    sealed interface CharClass extends RegExp
    {
        record Single(int ch) implements CharClass
        {
            public Single
            {
                validate(ch, "ch");
            }

            static void validate(int ch, String name)
            {
                if(!Character.isValidCodePoint(ch))
                    throw new IllegalArgumentException(name+" is not a valid code point: "+Integer.toHexString(ch));
                if(Character.MIN_SURROGATE<=ch && ch<=Character.MAX_SURROGATE)
                    throw new IllegalArgumentException(name+" is a lone surrogate: "+Integer.toHexString(ch));
            }
        }

        record Range(int from, int to) implements CharClass
        {
            public Range
            {
                Single.validate(from, "from");
                Single.validate(to, "to");

                if(!(from <= to))   // [Z-A] is illegal
                    throw new IllegalArgumentException("required: from<=to");
            }
        }

        // if arity=0, it means empty set of chars.
        record Union(CharClass... args) implements CharClass
        {
        }

        // if arity=0, it means all chars.
        record Intersection(CharClass... args) implements CharClass
        {
        }

        record Negation(CharClass arg) implements CharClass
        {
        }


        // predefined char classes are convenient for crafting regex manually;
        // not that much useful in our library.
        //
        // we don't provide constants for predefined
        //     . \d \s \N{name} \p{Lower} etc
        // they should be explicitly defined in user grammar,
        //
        // in any case, if needed, user can do
        //     var digit  = predefined("\\d");
        //     var pLower = predefined("\\p{Lower}");
        //
        // \R, \X  - not char class. [\R] is illegal
        //
        record Predefined(String regex) implements CharClass
        {
            public Predefined
            {
                PkgUtil.validateRegex(regex);
                // we are not validating that `regex` is indeed a predefined char class. (how?)
                // we assume that it's "pAtomic" in ToRegex; if not, error is delayed.
            }
        }

    } // CharClass




    // arity=0: matches nothing.
    record Alternation(RegExp... args) implements RegExp
    {
    }

    // arity=0: epsilon, matches ""
    record Concatenation(RegExp... args) implements RegExp
    {
    }

    record Quantified(RegExp arg, long min, long max, char greediness) implements RegExp
    {
        // there is no way max=Long.MAX means a finite bound. not so sure about Int.MAX
        public static final long INFINITY = Long.MAX_VALUE;

        public static final char greedy = '\u0000';
        public static final char reluctant = '?';
        public static final char possessive = '+';

        public Quantified
        {
            if(!(0<= min && min <= max))
                throw new IllegalArgumentException("invalid min-max: "+ min +"-"+ max);
        }
    }



    // we don't use an enum type containing all known boundaries;
    // new ones could come up, e.g. \b{g}
    record Boundary(String regex) implements RegExp
    {
        public Boundary
        {
            PkgUtil.validateRegex(regex);
        }
    }


    // positive/negative look ahead/behind
    record Lookaround(RegExp arg, boolean ahead, boolean positive) implements RegExp
    {
    }



    sealed interface BackReference extends RegExp
    {
        // problem:  \1 to \9 is always interpreted as backreference
        //     \10, \11, etc is interpreted as backreference only if group 10 etc exists.
        //     otherwise, it's interpreted as (\1)0, i.e. sequence of \1 and '0'
        record WithNumber(int number) implements BackReference
        {
            public WithNumber
            {
                // 0 is not allowed. can't back-ref group 0 inside group 0.
                // also, \0 would be interpreted as start of octal escape
                if(number<=0)
                    throw new IllegalArgumentException("invalid group number: "+number);
            }
        }

        record WithName(String name) implements BackReference
        {
            public WithName
            {
                Group.Named.validateName(name);
            }
        }
    }


    // capturing group, named or unnamed.
    sealed interface Group extends RegExp
    {
        RegExp arg();

        record Unnamed(RegExp arg) implements Group
        {
        }

        record Named(RegExp arg, String name) implements Group
        {
            public Named
            {
                validateName(name);
            }
            public static void validateName(String name)
            {
                if(!PkgUtil.isValidGroupName(name))
                    throw new IllegalArgumentException("invalid group name: "+ name);
            }
        }
    }

    
    // as in javadoc:  (?>X) ... X, as an independent, non-capturing group
    record AtomicGroup(RegExp arg) implements RegExp
    {
    }



    // if a flag is specified to be both on and off, it's equiv to not being specified.
    // if no flags are specified at all, this construct produces (?:X) syntactically,
    //    which is just a non-capturing group; not much use in our library.
    record Flagged(RegExp arg, String onFlags, String offFlags) implements RegExp
    {
        // do not check validity of flags here; accept all.
    }


    // an opaque piece of regex.
    // user could use it for new constructs like \R, \X
    // we don't know it's precedence; often it'll be enclosed in non-capturing group.
    // Do not use opaque for a char class; use predefined instead.
    record Opaque(String regex) implements RegExp
    {
        public Opaque
        {
            PkgUtil.validateRegex(regex);
        }
    }


}
