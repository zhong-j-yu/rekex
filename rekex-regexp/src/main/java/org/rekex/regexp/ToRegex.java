package org.rekex.regexp;

import org.rekex.common_util.SwitchOnType;
import org.rekex.regexp.RegExp.*;
import org.rekex.regexp.RegExp.CharClass.*;

import java.util.function.Function;

import static org.rekex.regexp.PkgUtil.esc;

class ToRegex
{
    public static String regex(RegExp exp)
    {
        return strChild(pRegex, exp);
    }

    // a regex string and its syntactic precedence
    record PS(int p, String s){}

    static int precedence = 0;

    static final int pAtomic = precedence--;
    static final int pQuant = precedence--;
    static final int pConcat = precedence--;
    static final int pAlt = precedence--;

    static final int pRegex = pAlt;

    // char class not yet enclosed by []
    static final int pRange = precedence--;
    static final int pUnion = precedence--;
    static final int pIntersect = precedence--;

    static String strChild(int parentPrecedence, RegExp child)
    {
        var ps = toPs.apply(child);
        return (ps.p < parentPrecedence) ? addParens(ps) : ps.s;
    }
    static String addParens(PS ps)
    {
        if(ps.p<=pRange)
            return "[" + ps.s + "]";   // brackets around char class

        return "(?:" + ps.s + ")"; // non-capturing group
        // it might be safe to use capturing groups, e.g. when there's
        // no Group in exp anyways, so user won't care about group numbering.
        // but capturing groups require more resources for Matcher.
    }
    static PS join(int parentPrecedence, String op, RegExp[] args)
    {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<args.length; i++)
        {
            if(i>0) sb.append(op);
            sb.append(strChild(parentPrecedence, args[i]));
        }
        return new PS(parentPrecedence, sb.toString());
    }



    static final Function<RegExp,PS> toPs =
        new SwitchOnType<RegExp,PS>()
            .on(Single.class, ToRegex::onSingle)
            .on(Range.class, ToRegex::onRange)
            .on(Union.class, ToRegex::onUnion)
            .on(Intersection.class, ToRegex::onIntersection)
            .on(Negation.class, ToRegex::onNegation)
            .on(Predefined.class, ToRegex::onPredefined)
            .on(Alternation.class, ToRegex::onAlternation)
            .on(Concatenation.class, ToRegex::onConcatenation)
            .on(Quantified.class, ToRegex::onQuantified)
            .on(Boundary.class, ToRegex::onBoundary)
            .on(Lookaround.class, ToRegex::onLookaround)
            .on(BackReference.WithNumber.class, ToRegex::onBackReferenceWithNumber)
            .on(BackReference.WithName.class, ToRegex::onBackReferenceWithName)
            .on(Group.Unnamed.class, ToRegex::onGroupUnnamed)
            .on(Group.Named.class, ToRegex::onGroupNamed)
            .on(AtomicGroup.class, ToRegex::onAtomicGroup)
            .on(Flagged.class, ToRegex::onFlagged)
            .on(Opaque.class, ToRegex::onOpaque)
            .complete(RegExp.class);

    static PS onSingle(Single r)
    {
        String s = esc(r.ch());
        return new PS(pAtomic, s);
    }

    static PS onRange(Range r)
    {
        String s = esc(r.from()) + "-" + esc(r.to());
        return new PS(pRange, s);
    }

    static PS onUnion(Union r)
    {
        var args = r.args();
        if(args.length==0) // can't produce "[]". use intersection to create empty set
            return new PS(pIntersect, "0&&1");
        if(args.length==1)
            return toPs.apply(args[0]);
        return join(pUnion, "", args);
    }


    static PS onIntersection(Intersection r)
    {
        var args = r.args();
        if(args.length==0) // all chars
            return onRange(new Range(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT));
        if(args.length==1)
            return toPs.apply(args[0]);
        return join(pIntersect, "&&", args);
    }

    static PS onNegation(Negation r)
    {
        String s = toPs.apply(r.arg()).s;
        s = "[^" + s + "]";
        return new PS(pAtomic, s);
    }

    static PS onPredefined(Predefined r)
    {
        return new PS(pAtomic, r.regex());
    }

    static PS onAlternation(Alternation r)
    {
        var args = r.args();
        if(args.length==0) // match nothing
            return new PS(pAtomic, "(?!)");
        if(args.length==1)
            return toPs.apply(args[0]);
        return join(pAlt, "|", args);
    }

    static PS onConcatenation(Concatenation r)
    {
        var args = r.args();

        if(args.length==0) // match empty input
            return new PS(pConcat, "");
        // "" works if `r` is inside Alt, Group;
        // but inside Quant it requires parens

        if(args.length==1)
            return toPs.apply(args[0]);
        return join(pConcat, "", args);
    }

    static PS onQuantified(Quantified r)
    {
        // we also need to paren arg, if it's a Quant too (e.g. X+),
        // otherwise we'll produce something like X++, which means something else
        String s = strChild(1+pQuant, r.arg());
        final long min=r.min(), max=r.max(), INF=Quantified.INFINITY;
        if (min == 0 && max == 1)
            s += "?";
        else if (min == 0 && max == INF)
            s += "*";
        else if (min == 1 && max == INF)
            s += "+";
        else if (min == max)
            s += "{" + min + "}";
        else if (max == INF)
            s += "{" + min + ",}";
        else
            s += "{" + min + "," + max + "}";

        if(r.greediness() !=0)
            s += (char) r.greediness();

        return new PS(pQuant, s);
    }

    static PS onBoundary(Boundary r)
    {
        return new PS(pAtomic, r.regex());
    }

    static PS onLookaround(Lookaround r)
    {
        String s = regex(r.arg());
        s = beginningOf(r) + s + ")";
        return new PS(pAtomic, s);
    }
    static String beginningOf(Lookaround r)
    {
        return "(?"
            + (r.ahead() ?    ""  : "<")
            + (r.positive() ? "=" : "!");
    }

    static PS onBackReferenceWithNumber(BackReference.WithNumber r)
    {
        String s = "\\"+ r.number();
        return new PS(pAtomic, s);
    }

    static PS onBackReferenceWithName(BackReference.WithName r)
    {
        String s = "\\k<"+ r.name() + ">";
        return new PS(pAtomic, s);
    }

    static PS onGroupUnnamed(Group.Unnamed r)
    {
        String s = regex(r.arg());
        s = "(" + s +")";
        return new PS(pAtomic, s);
    }

    static PS onGroupNamed(Group.Named r)
    {
        String s = regex(r.arg());
        s = "(?<" + r.name() + ">" + s + ")";
        return new PS(pAtomic, s);
    }

    static PS onAtomicGroup(AtomicGroup r)
    {
        String s = regex(r.arg());
        s = "(?>" + s + ")";
        return new PS(pAtomic, s);
    }

    static PS onFlagged(Flagged r)
    {
        String s = regex(r.arg());
        s = "(?" + flagsStr(r) + ":" + s + ")";
        return new PS(pAtomic, s);
    }
    static String flagsStr(Flagged r)
    {
        String flags = r.onFlags();
        if(!r.offFlags().isEmpty())
            flags = flags + "-" + r.offFlags();
        return flags;
    }


    public static PS onOpaque(Opaque r)
    {
        return new PS(pRegex, r.regex());
    }
}
