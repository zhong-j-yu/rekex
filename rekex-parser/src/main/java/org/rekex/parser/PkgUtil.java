package org.rekex.parser;

import org.rekex.grammar.Instantiator;

import javax.tools.*;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class PkgUtil
{
    // if a ctor contains semantic predicates, it must declare failure types
    // in the `throws` clause. a ctor with semantic predicate is a production rule
    // with extra constraints unintelligible to the parser; the parser must invoke
    // the ctor to know whether the rule matches, or whether to try next alternative.
    //
    // most ctors are "pure", without semantic predicates;
    // the parser knows they are pure by looking at the throws clauses.
    // pure ctors are deterministic to the parser; their invocations can
    // be delayed till the end of parsing process.
    // usually a pure ctor will not throw;
    // but if it does throw, it'll be considered a Fatal error, unrecoverable.
    static List<Class<? extends Exception>> getDeclaredExceptions(Instantiator ins)
    {
        Class<?>[] declared;
        if(ins instanceof Instantiator.NewInstance m)
            declared = m.constructor().getExceptionTypes();
        else if(ins instanceof Instantiator.StaticMethod m)
            declared = m.method().getExceptionTypes();
        else if(ins instanceof Instantiator.InstanceMethod m)
            declared = m.method().getExceptionTypes();
        else if(ins instanceof Instantiator.StaticField f)
            declared = new Class<?>[]{};
        else
            throw new AssertionError();

        // only subtypes of Exception
        List<Class<? extends Exception>> list = new ArrayList<>();
        for(var clazz : declared)
        {
            if(Exception.class.isAssignableFrom(clazz))
                list.add((Class<? extends Exception>)clazz);
            else if(Error.class.isAssignableFrom(clazz))
                ;
            else
                throw new UnsupportedOperationException("bad throws type: "+clazz+" from "+ins);
        }
        return list;
    }

    // compile given .java files, output .class files under outDir.
    static void jc_compile(List<Path> javaFiles, Path outDir, List<String> options) throws Exception
    {
        if(javaFiles.isEmpty())
            throw new IllegalArgumentException("empty javaFiles");

        ArrayList<File> javaFilesF = new ArrayList<>();
        for(Path path : javaFiles)
            javaFilesF.add(path.toFile());
        File outDirF = outDir.toFile();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler(); // must be a new instance
        // it's not null. we tested in constructor.

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        Files.createDirectories(outDir);
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(outDirF)); // must be one dir

        StringWriter sw = new StringWriter();  // for javac messages

        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(javaFilesF);
        JavaCompiler.CompilationTask task = compiler.getTask(sw, fileManager, null, options, null, compilationUnits);
        Boolean ok = task.call();
        String javacMsg = sw.toString();

        fileManager.close();

        if(!ok.booleanValue())
        {
            final String NL = System.lineSeparator();
            final String HR = "------------------------------------------------------------------------------";
            throw new Exception("javac fails"+NL+NL+HR+NL+NL+javacMsg+NL+HR+NL);
        }
    }

    static Class<?> loadClass(String className, ClassLoader parentCL, Path outDir) throws Exception
    {
        URL urls =   outDir.toAbsolutePath().normalize().toUri().toURL(); // throws
        var classLoader = new OurClassLoader(urls, parentCL);
        return Class.forName(className, false, classLoader);
    }

    // Find classes in our dir *before* parent classloader, violating the common practice.
    // During development, user may have previously generated .java file in their source dir,
    // which gets compiled and loadable from the parent classloader in the runtime environment.
    // But we must load the freshly compiled class from our dir, which is more up-to-date.
    static class OurClassLoader extends URLClassLoader
    {
        public OurClassLoader(URL ourUrl, ClassLoader parent)
        {
            super(new URL[]{ourUrl}, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            synchronized (getClassLoadingLock(name))
            {
                Class<?> c = findLoadedClass(name);
                if(c==null)
                {
                    try
                    {
                        c = findClass(name);
                    }
                    catch (ClassNotFoundException e)
                    {
                        // c==null
                    }
                }
                if(c!=null)
                {
                    if (resolve)
                        resolveClass(c);
                    return c;
                }
                return super.loadClass(name, resolve);
            }
        }
    }

}
