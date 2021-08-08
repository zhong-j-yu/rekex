package org.rekex.regexp;

import org.rekex.common_util.SwitchOnType;
import org.rekex.regexp.RegExp.*;
import org.rekex.regexp.RegExp.CharClass.*;

import java.util.function.Function;

class ToTreeText
{
    StringBuilder sb = new StringBuilder();

    public static String toTreeText(RegExp exp)
    {
        var thiz = new ToTreeText();
        thiz.print2(exp, "", EOL);
        return thiz.sb.toString();
    }

    static final String TAB = " ".repeat(4);
    static final String EOL = System.lineSeparator();

    // return Object: meaningless and ignored; can return anything.

    // indent1: for the 1st line  of exp  e.g. "|-- "
    // indent2: for rest of lines of exp  e.g. "|   "
    Object print2(RegExp exp, String indent1, String indent2)
    {
        sb.append(indent1);
        return toTree.apply(exp).apply(indent2);
    }
    Object print1(RegExp exp, String indent)
    {
        return print2(exp, indent, indent);
    }

    Object print3(RegExp[] args, String indent1, String indent2, String indent2L)
    {
        for(int i=0; i<args.length; i++)
        {
            var arg = args[i];

            // display adjacent chars/ranges on the same line to reduce space
            // whether their relationships are (seq, alt, union) is apparent from the parent
            if(arg instanceof Single single)
            {
                if(i>0 && args[i-1] instanceof Single)
                {
                    appendSingle(single);
                    continue;
                }
            }
            else if(arg instanceof Range r)
            {
                if(i>0 && args[i-1] instanceof Range)
                {
                    appendRange(r);
                    continue;
                }
            }

            boolean last = i==args.length-1;
            print2(arg, indent1, last?indent2L:indent2);
        }
        return sb;
    }


    final Function<RegExp, Function<String,Object>> toTree =
        new SwitchOnType<RegExp, Function<String,Object>>()
            .on(Single.class, r->s->onSingle(r, s))
            .on(Range.class, r->s->onRange(r, s))
            .on(Union.class, r->s->onUnion(r, s))
            .on(Intersection.class, r->s->onIntersection(r, s))
            .on(Negation.class, r->s->onNegation(r, s))
            .on(Predefined.class, r->s->onPredefined(r, s))
            .on(Alternation.class, r->s->onAlternation(r, s))
            .on(Concatenation.class, r->s->onConcatenation(r, s))
            .on(Quantified.class, r->s->onQuantified(r, s))
            .on(Boundary.class, r->s->onBoundary(r, s))
            .on(Lookaround.class, r->s->onLookaround(r, s))
            .on(BackReference.WithNumber.class, r->s->onBackReferenceWithNumber(r, s))
            .on(BackReference.WithName.class, r->s->onBackReferenceWithName(r, s))
            .on(Group.Unnamed.class, r->s->onGroupUnnamed(r, s))
            .on(Group.Named.class, r->s->onGroupNamed(r, s))
            .on(AtomicGroup.class, r->s->onAtomicGroup(r, s))
            .on(Flagged.class, r->s->onFlagged(r, s))
            .on(Opaque.class, r->s->onOpaque(r, s))
            .complete(RegExp.class);


    Object onSingle(Single single, String indent2)
    {
        // plural: we could put more following chars on the same line;
        // even if it's just one char, it is actually a char class/set (of one).
        sb.append("chars ");
        return appendSingle(single);
    }
    Object appendSingle(Single single)
    {
        return sb.append(esc2(single.ch()));
    }

    Object onRange(Range r, String indent2)
    {
        sb.append("range ");
        return appendRange(r);
    }
    Object appendRange(Range r)
    {
        return sb
            .append("[")
            .append(esc2(r.from()))
            .append("-")
            .append(esc2(r.to()))
            .append("]")
            ;
    }

    Object onUnion(Union union, String indent2)
    {
        sb.append("union");
        return print3(union.args(), indent2+" :--", indent2+" :  ", indent2+TAB);
    }

    Object onIntersection(Intersection intersect, String indent2)
    {
        sb.append("intersect");
        return print3(intersect.args(), indent2+" && ", indent2+" .  ", indent2+TAB);
    }

    Object onNegation(Negation neg, String indent2)
    {
        sb.append("negate");
        return print1(neg.arg(), indent2+TAB);
    }

    Object onPredefined(Predefined pd, String indent2)
    {
        sb.append("predefined: ");
        return appendRegex(pd);
    }

    Object onAlternation(Alternation alt, String indent2)
    {
        sb.append("alt");
        return print3(alt.args(), indent2+" :--", indent2+" :  ", indent2+ TAB);
    }

    Object onConcatenation(Concatenation seq, String indent2)
    {
        if(seq.args().length==0)
            return sb.append("epsilon");
        sb.append("seq");
        return print3(seq.args(), indent2+" |--", indent2+" |  ", indent2+ TAB);
    }

    Object onQuantified(Quantified q, String indent2)
    {
        sb.append(quantDesc(q));
        return print1(q.arg(), indent2+TAB);
    }

    Object onBoundary(Boundary r, String indent2)
    {
        sb.append("boundary: ");
        return appendRegex(r);
    }

    Object onLookaround(Lookaround look, String indent2)
    {
        sb.append("look_")
            .append(look.ahead()?"ahead ":"behind ")
            .append(look.positive()? "positive":"negative")
            .append(ToRegex.beginningOf(look))
        ;
        return print1(look.arg(), indent2+TAB);
    }

    Object onBackReferenceWithNumber(BackReference.WithNumber r, String indent2)
    {
        sb.append("back_ref: ");
        return appendRegex(r);
    }

    Object onBackReferenceWithName(BackReference.WithName r, String indent2)
    {
        sb.append("back_ref: ");
        return appendRegex(r);
    }

    Object onGroupUnnamed(Group.Unnamed group, String indent2)
    {
        sb.append("group(");
        return print1(group.arg(), indent2+TAB);
    }

    Object onGroupNamed(Group.Named group, String indent2)
    {
        sb.append("group(?<").append(group.name()).append(">");
        return print1(group.arg(), indent2+TAB);
    }

    Object onAtomicGroup(AtomicGroup group, String indent2)
    {
        sb.append("atomic_group(?>");
        return print1(group.arg(), indent2+TAB);
    }

    Object onFlagged(Flagged f, String indent2)
    {
        sb.append("flag(?").append(ToRegex.flagsStr(f)).append(":");
        return print1(f.arg(), indent2+TAB);
    }

    Object onOpaque(Opaque opaque, String indent2)
    {
        sb.append("opaque: ");
        return appendRegex(opaque);
    }






    Object appendRegex(RegExp exp)
    {
        return sb.append(ToRegex.regex(exp)); // do we need to escape it?
    }


    // does not escape visible chars
    static String esc2(int ch)
    {
        if(' '==ch)
            return "\\x20";
        if(0x21<=ch && ch<=0x7E)
            return ""+ch;
        return PkgUtil.esc(ch);
    }

    static String quantDesc(Quantified q)
    {
        var INFINITY = Quantified.INFINITY;
        var _min = q.min();
        var _max = q.max();
        var _greediness = q.greediness();

        String greed = "";
        if(_greediness != Quantified.greedy)
        {
            greed += (char) _greediness;
            greed += (_greediness =='?') ? " reluctant" : " possessive";
        }

        if (_min == 0 && _max == 1)
            return "opt?" + greed;
        if (_min == 0 && _max == INFINITY)
            return "rep0*" + greed;
        if (_min == 1 && _max == INFINITY)
            return "rep1+" + greed;
        if (_min == _max)
            return "times{" + _min + "}" + greed;
        if (_max == INFINITY)
            return "times{" + _min + ",}" + greed;

        return "times{" + _min + "," + _max + "}" + greed;
    }

}
