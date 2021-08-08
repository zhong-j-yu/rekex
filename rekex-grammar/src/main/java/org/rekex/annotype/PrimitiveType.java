package org.rekex.annotype;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Set;

/**
 * Primitive type.
 */
final public class PrimitiveType extends AnnoType
{
    // the Class object representing the primitive type, e.g. char.class.
    final Class<?> clazz;

    public PrimitiveType(Set<Annotation> annotations, Class<?> clazz)
    {
        super(annotations);
        this.clazz = clazz;
    }

    /**
     * The Class representing the primitive type, for example `char.class`.
     */
    public Class<?> clazz()
    {
        return clazz;
    }

    public ClassType boxed()
    {
        var c2 = MethodType.methodType(clazz).wrap().returnType();
        return new ClassType(annotations, c2, List.of());
    }

    @Override
    int sub_hashCode()
    {
        return clazz.hashCode();
    }

    @Override
    boolean sub_equals(AnnoType obj)
    {
        PrimitiveType that = (PrimitiveType)obj;
        return this.clazz == that.clazz;
    }

    @Override
    void buildString(boolean full, StringBuilder sb)
    {
        buildAnnoString(full, sb);
        sb.append(clazz.getCanonicalName()); // e.g. "char"
    }
}
