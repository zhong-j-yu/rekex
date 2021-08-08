package org.rekex.helper.anno;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.spec.SizeLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * `@AtMost(n)` is equivalent to {@link SizeLimit @SizeLimit(max=n)}.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtMost
{
    int value();

    AnnoMacro<AtMost, SizeLimit> toRange = thiz ->
        AnnoBuilder.build(SizeLimit.class, SizeLimit::max, thiz.value());

}
