package org.rekex.regexp;

import org.rekex.common_util.SwitchOnType;
import org.rekex.regexp.RegExp.*;

import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

class ToCountGroup
{
    final Group target;

    public ToCountGroup(Group target)
    {
        this.target = target;
    }

    // find the group number of `group` in `exp`.
    // if not found, return -1.
    public static int findGroupNumberIn(Group group, RegExp exp)
    {
        int count = new ToCountGroup(group).count(exp, 0);
        return count<0 ? ~count : -1;
    }

    // depth first search on tree nodes, ++count on each group;
    // return ~count when target group is found.

    final Function<RegExp, IntUnaryOperator> count =
        new SwitchOnType<RegExp, IntUnaryOperator>()
            .on(Group.class, r -> c -> {
                ++c;
                return targetMatches(r) ? ~c // found; terminate.
                    : count(r.arg(), c);
            })
            .on(Opaque.class, r -> c -> {
                var pattern = Pattern.compile(r.regex());
                var c0 = pattern.matcher("").groupCount();
                return c + c0;
            })
            .on(Alternation.class,
                r -> c -> countChildren(r.args(), c)
            )
            .on(Concatenation.class,
                r -> c -> countChildren(r.args(), c)
            )
            .on(Quantified.class,
                r -> c -> count(r.arg(), c)
            )
            .on(Lookaround.class,
                r -> c -> count(r.arg(), c)
            )
            .on(AtomicGroup.class,
                r -> c -> count(r.arg(), c)
            )
            .on(Flagged.class,
                r -> c -> count(r.arg(), c)
            )
            .on(CharClass.class,
                r -> c -> c
            )
            .on(Boundary.class,
                r -> c -> c
            )
            .on(BackReference.class,
                r -> c -> c
            )
            .complete(RegExp.class);

    int count(RegExp r, int c)
    {
        return count.apply(r).applyAsInt(c);
    }

    boolean targetMatches(Group g)
    {
        return target==g;
    }

    int countChildren(RegExp[] children, int count)
    {
        for(var child : children)
        {
            count = count(child, count);
            if(count<0)
                break;
        }
        return count;
    }

}
