package org.rekex.grammar;

import org.opentest4j.AssertionFailedError;
import org.rekex.annotype.AnnoType;
import org.rekex.annotype.ArrayType;
import org.rekex.annotype.ClassType;
import org.rekex.annotype.PrimitiveType;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GrammarTestBase
{
    static AnnoType typeOf(Class<?> clazz)
    {
        if(clazz.isPrimitive())
            return new PrimitiveType(Set.of(), clazz);
        if(clazz.isArray())
            return new ArrayType(Set.of(), typeOf(clazz.getComponentType()));
        return ClassType.withTypeVars(clazz);
    }

    public static Grammar derive(Class<?> clazz) throws Exception
    {
        return derive(clazz, null);
    }
    public static Grammar derive(Class<?> clazz, Class<?> catalogClass) throws Exception
    {

        var clazzType = typeOf(clazz);
        return Grammar.deriveFrom(List.of(clazzType), catalogClass);
    }
    public static void dump(Grammar grammar)
    {
        System.out.println(grammar.toText());
    }

    public static Exception failOn(Class<?> clazz, String msg)
    {
        return failOn(clazz, null, msg);
    }
    public static Exception failOn(Class<?> clazz, Class<?> catalogClass, String msg)
    {
        Exception ex = assertThrows(Exception.class, ()->{
            var grammar = derive(clazz, catalogClass);
            dump(grammar); // should not reach here
        });
        if(ex==null)
            throw new AssertionFailedError("exception expected but not raised");

        Throwable x = ex;
        while(x!=null)
        {
            if(x.toString().contains(msg))
                return ex;
            x = x.getCause();
        }
        throw new AssertionFailedError("exception does not contain msg:"+msg, ex);
    }
}
