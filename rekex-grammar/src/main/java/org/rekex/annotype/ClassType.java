package org.rekex.annotype;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * Class or interface type.
 * <p>
 *     Raw types are not supported; if the class is generic, `typeArgs` must be supplied.
 * </p>
 */
public final class ClassType extends RefType
{
    final Class<?> clazz;
    final List<TypeArg> typeArgs;

    public ClassType(Set<Annotation> annotations, Class<?> clazz, List<TypeArg> typeArgs)
    {
        super(annotations);

        if(clazz.getTypeParameters().length!=typeArgs.size())
            throw new IllegalArgumentException("wrong number of type args: "+clazz);

        this.clazz = clazz;
        this.typeArgs = typeArgs;
    }

    // use typeVars as typeArgs, if any.
    public static ClassType withTypeVars(Class<?> clazz)
    {
        var typeVars0 = TypeMath.getTypeVars(clazz);
        @SuppressWarnings("unchecked")
        List<TypeArg> typeVars = (List<TypeArg>)(List<?>)typeVars0;
        // annotations declared on the class are not type annotations
        return new ClassType(Set.of(), clazz, typeVars);
    }

    // no annotations, no type arguments
    public static ClassType of(Class<?> clazz)
    {
        return new ClassType(Set.of(), clazz, List.of());
    }

    /**
     * Get the class or interface of this type.
     */
    public Class<?> clazz()
    {
        return clazz;
    }

    /**
     * Get the class or interface of this type.
     */
    public List<TypeArg> typeArgs()
    {
        return typeArgs;
    }

    @Override
    int sub_hashCode()
    {
        return 31 * clazz.hashCode() + typeArgs().hashCode();
    }

    @Override
    boolean sub_equals(AnnoType obj)
    {
        ClassType that = (ClassType)obj;
        return this.clazz == that.clazz
            && this.typeArgs.equals(that.typeArgs);
    }

    @Override
    void buildString(boolean full, StringBuilder sb)
    {
        // https://docs.oracle.com/javase/specs/jls/se16/html/jls-4.html#jls-ClassType
        // annotation can only appear in front of the simple name
        //    wrong:   @A()p.C.D
        //    right:   p.C.@A()D
        String sn = PkgUtil.simpleName(clazz);
        if(full)
        {
            String cn = PkgUtil.canonName(clazz);
            sb.append(cn, 0, cn.length()-sn.length());
        }
        buildAnnoString(full, sb);
        sb.append(sn);
        if(typeArgs.isEmpty())
            return;
        sb.append('<');
        for(int i=0; i< typeArgs.size(); i++)
        {
            if(i>0)
                sb.append(',');
            typeArgs.get(i).asRefType().buildString(full, sb);
        }
        sb.append('>');
    }
}
