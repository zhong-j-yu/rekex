package org.rekex.grammar;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.bytecode.SourceFileAttribute;
import org.rekex.grammar.GrammarBuilder.CtorInfo;
import org.rekex.spec.Ctor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

class PkgUtil
{
    public static ArrayList<CtorInfo> orderCtors(ArrayList<CtorInfo> ctorList) throws Exception
    {
        if(ctorList.size()<2)
            return ctorList;

        int countOrderSpecified = 0;
        for(var info : ctorList)
        {
            var ctor = info.ctor();
            var anno = ctor.getAnnotation(Ctor.class);
            if(anno!=null && anno.value()!=Ctor.ORDER_UNSPECIFIED)
                countOrderSpecified++;
        }

        if(countOrderSpecified>0)
        {
            if(countOrderSpecified!=ctorList.size())
                throw new Exception("some ctors have order unspecified: "+ctorList);

            ctorList.sort(Comparator.comparingInt(e -> e.ctor().getAnnotation(Ctor.class).value()));
            return ctorList;
        }

        // check line numbers

        Class<?> clazz = ctorList.get(0).ctor().getDeclaringClass();
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(clazz));
        CtClass cc = pool.get(clazz.getName());

        HashMap<Executable, Integer> lineNumberMap = new HashMap<>();
        for(var info : ctorList)
        {
            var ctor = info.ctor();
            int lineNumber = getLineNumber(pool, cc, ctor);
            if(lineNumber==-1)
                throw new Exception("lineNumber unknown: "+ctor);
            lineNumberMap.put(ctor, lineNumber);
        }
        ctorList.sort(Comparator.comparingInt(info->lineNumberMap.get(info.ctor())));
        return ctorList;
    }

    static int getLineNumber(ClassPool pool, CtClass cc, Executable exec) throws Exception
    {
        Class<?>[] paramTypesA = exec.getParameterTypes();
        CtClass[] paramTypesB = new CtClass[paramTypesA.length];
        for(int i=0; i< paramTypesA.length; i++)
            paramTypesB[i] = pool.get(paramTypesA[i].getName());

        CtBehavior ctor;
        if(exec instanceof Constructor<?> constructor)
            ctor = cc.getDeclaredConstructor(paramTypesB);
        else if(exec instanceof Method method)
            ctor = cc.getDeclaredMethod(method.getName(), paramTypesB);
        else
            throw new AssertionError();

        return ctor.getMethodInfo().getLineNumber(0);
    }

    public static void orderSealedSubclasses(Class<?> clazz, Class<?>[] subclasses) throws Exception
    {
        if(subclasses.length<2)
            return;

        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(clazz));

        // they must be in the same source file.
        String file = getSourceFile(pool, clazz);
        if(file==null)
            throw new Exception("source file unknown: "+clazz);

        HashMap<Class<?>, Integer> lineNumberMap = new HashMap<>();
        for(var subclass : subclasses)
        {
            var f = getSourceFile(pool, subclass); // unlikely to be null
            if(!file.equals(f))
                throw new Exception("not in the same file:" + subclass);

            int lineNumber = getLineNumber(pool, subclass);
            if(lineNumber==-1)
                throw new Exception("lineNumber unknown: "+subclass);
            lineNumberMap.put(subclass, lineNumber);
        }
        Arrays.sort(subclasses, Comparator.comparingInt(lineNumberMap::get));
    }

    static String getSourceFile(ClassPool pool, Class<?> clazz) throws Exception
    {
        var cc = pool.get(clazz.getName());
        var cf = cc.getClassFile();
        SourceFileAttribute sf = (SourceFileAttribute)(cf.getAttribute(SourceFileAttribute.tag));
        if(sf==null) return null;
        var fn = sf.getFileName();
        // it's just the file name; no path info. add the path
        var pkg = clazz.getPackageName().replace('.', '/');
        return pkg + "/" + fn;
    }

    static int getLineNumber(ClassPool pool, Class<?> clazz) throws Exception
    {
        var cc = pool.get(clazz.getName());
        int n = getLineNumber(cc.getDeclaredBehaviors());
        if(n!=-1)
            return n;

        // no methods or constructors? try our luck iwth static init
        var init = cc.getClassInitializer();
        if(init!=null)
            n = getLineNumber(init);

        return n;
    }

    static int getLineNumber(CtBehavior... bs)
    {
        for(var b : bs)
        {
            int n = b.getMethodInfo().getLineNumber(0);
            if(n!=-1)
                return n;
        }
        return -1;
    }

}
