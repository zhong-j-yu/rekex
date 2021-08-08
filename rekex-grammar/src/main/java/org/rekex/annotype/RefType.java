package org.rekex.annotype;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Reference type.
 */
public sealed abstract class RefType
    extends AnnoType
    implements TypeArg
    permits ArrayType, ClassType, TypeVar
{
    RefType(Set<Annotation> annotations)
    {
        super(annotations);
    }
}
