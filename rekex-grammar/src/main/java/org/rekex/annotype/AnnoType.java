package org.rekex.annotype;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Annotated type.
 */
public sealed abstract class AnnoType
    permits PrimitiveType, RefType
{
    // order doesn't matter; they are conjunctive.
    final Set<Annotation> annotations;

    AnnoType(Set<Annotation> annotations)
    {
        this.annotations = annotations;
    }

    public Set<Annotation> annotations()
    {
        return annotations;
    }

    int hashCode=0; // caching
    @Override
    public int hashCode()
    {
        int h = hashCode;
        if(h==0)
        {
            h = annotations.hashCode();
            h = 31*h + sub_hashCode();
            hashCode = h;
        }
        return h;
    }
    // subclass hash (sans annotations)
    abstract int sub_hashCode();


    @Override
    public boolean equals(Object that)
    {
        return this == that
            || that != null
            && this.getClass() == that.getClass()
            && this.annotations.equals(((AnnoType)that).annotations)
            && this.sub_equals((AnnoType)that);
    }
    // subclass equality (sans annotations)
    abstract boolean sub_equals(AnnoType obj);


    /**
     * A textual description  of the type.
     * <p>
     *     This method is equivalent to {@link #toString(boolean) toString(true)}.
     * </p>
     */
    @Override
    public String toString(){ return toString(true); }

    /**
     * A textual description  of the type.
     * <p>
     *     If `full==false`, simpler names are used for types.
     * </p>
     */
    // we require that the string is a valid piece of java code,
    // and can be compiled back to the same annotated type
    public String toString(boolean full)
    {
        //we are not caching the result. usually it's only used in diagnostics.
        StringBuilder sb = new StringBuilder();
        buildString(full, sb);
        return sb.toString();
    }

    void buildAnnoString(boolean full, StringBuilder sb)
    {
        for(var anno : annotations)
        {
            PkgUtil.buildString(full, anno, sb);
        }
    }

    abstract void buildString(boolean full, StringBuilder sb);

}
