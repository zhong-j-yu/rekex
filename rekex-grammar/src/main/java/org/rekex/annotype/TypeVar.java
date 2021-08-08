package org.rekex.annotype;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.TypeVariable;
import java.util.Set;

/**
 * Type variable.
 */
// type variable declared in source code, and obtained by java.lang.reflect.
public final class TypeVar extends RefType
{
    final TypeVariable<?> jlrVar;
    // bounds are annotated bounds of jlrVar

    public TypeVar(Set<Annotation> annotations, TypeVariable<?> jlrVar)
    {
        super(annotations);
        this.jlrVar = jlrVar;
    }

    /**
     * Get the java.lang.reflect.TypeVariable of this type variable
     */
    public TypeVariable<?> jlrVar()
    {
        return jlrVar;
    }

    @Override
    int sub_hashCode()
    {
        return jlrVar.hashCode();
    }

    @Override
    boolean sub_equals(AnnoType obj)
    {
        TypeVar that = (TypeVar)obj;
        return this.jlrVar.equals(that.jlrVar);
    }

    @Override
    void buildString(boolean full, StringBuilder sb)
    {
        buildAnnoString(full, sb);
        if(!full)
        {
            sb.append(jlrVar.getName()); // hopefully its context is clear to caller
        }
        else // not very important to our use cases, where type vars are eventually substituted.
        {
            // JLS3#13.1 binary name of a type variable of a class: className$T
            GenericDeclaration host = jlrVar.getGenericDeclaration();
            // if host is method/constructor, host.toString() can be ugly, non-distinct and inaccurate.
            String hostStr = (host instanceof Class<?> clazz) ? clazz.getName() : host.toString();
            sb.append(hostStr).append('$').append(jlrVar.getName());
        }
    }
}
