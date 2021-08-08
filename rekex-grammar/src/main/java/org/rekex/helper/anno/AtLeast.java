package org.rekex.helper.anno;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.spec.SizeLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * `@AtLeast(n)` is equivalent to {@link SizeLimit @SizeLimit(min=n)}.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeast
{
    int value();

    AnnoMacro<AtLeast, SizeLimit> toRange = thiz ->
        AnnoBuilder.build(SizeLimit.class, SizeLimit::min, thiz.value());
}
