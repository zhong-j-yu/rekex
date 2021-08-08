package org.rekex.annotype;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Set;

/**
 * Array type. Could be primitive array.
 */
public final class ArrayType extends RefType
{
    final AnnoType componentType;

    public ArrayType(Set<Annotation> annotations, AnnoType componentType)
    {
        super(annotations);
        this.componentType = componentType;
    }

    /**
     * Get the component type of the array.
     */
    public AnnoType componentType()
    {
        return componentType;
    }

    @Override
    int sub_hashCode()
    {
        return 31 * ArrayType.class.hashCode() + componentType.hashCode();
    }

    @Override
    boolean sub_equals(AnnoType obj)
    {
        ArrayType that = (ArrayType)obj;
        return this.componentType.equals(that.componentType);
    }

    @Override
    void buildString(boolean full, StringBuilder sb)
    {
        // https://docs.oracle.com/javase/specs/jls/se16/html/jls-9.html#jls-9.7.4
        // source code:  @C int @A [] @B []
        // which means:  @A [ @B [ @C int ] ]
        ArrayList<Set<Annotation>> annoList = new ArrayList<>();
        AnnoType t0 = this;
        while(t0 instanceof ArrayType a)
        {
            annoList.add(a.annotations);
            t0 = a.componentType;
        }
        t0.buildString(full, sb);
        for(var annoSet : annoList)
        {
            for(var anno : annoSet)
                PkgUtil.buildString(full, anno, sb);
            sb.append("[]");
        }
    }
}
