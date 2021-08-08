package org.rekex.helper.datatype;

import org.rekex.helper.anno.AtLeast;

import java.util.List;

// note: doesn't work: @AtMost(N) OneOrMore<Foo>
//   if you need that, might as well do: @AtLeast(1)@AtMost(N)List<Foo>

/**
 * Equivalent to `@AtLeast(1) List&lt;T&gt;`
 */
public record OneOrMore<T>
    ( @AtLeast(1) List<T> values)
{
}
