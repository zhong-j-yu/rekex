package org.rekex.helper.anno;

import org.rekex.annomacro.AnnoMacro;
import org.rekex.common_util.AnnoBuilder;
import org.rekex.spec.SizeLimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * `@Size(n)` is equivalent to {@link SizeLimit @SizeLimit(min=n, max=n)}.
 */
@Target({ElementType.TYPE_USE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Size
{
    int value();

    AnnoMacro<Size, SizeLimit> toRange = thiz ->
        AnnoBuilder.build(SizeLimit.class,
            SizeLimit::min, thiz.value(), SizeLimit::max, thiz.value());
}
