package org.rekex.annotype;

//

/**
 * A convenient way to create an {@link AnnoType}.
 * Usage:
 * <pre>
 *     AnnoType type = new AnnoTypeToken&lt; @Anno()Type &gt;(){}.get();
 * </pre>
 */
public abstract class AnnoTypeToken<T>
{
    final AnnoType type;
    public AnnoTypeToken()
    {
        var sc = this.getClass().getAnnotatedSuperclass();
        ClassType ct = (ClassType)TypeMath.convertFromJlr(sc);
        this.type = ct.typeArgs.get(0).asRefType();
    }

    public AnnoType get()
    {
        return type;
    }
}
