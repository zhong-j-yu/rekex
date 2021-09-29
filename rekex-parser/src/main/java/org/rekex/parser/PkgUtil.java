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
import java.util.regex.Pattern;

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

        // modules ....
        //
        // if user app is unmodularized, everything is on the classpath, no problem.
        // apparently, JavaCompiler checks system property "java.class.path"
        // and use it for StandardLocation.CLASS_PATH
        //
        // however, if user app is modularized,
        // user modules and rekex modules are on the module-path, not classpath,
        // apparently, JavaCompiler does not inherit jdk.module.path
        // we have to explicitly add them.
        //
        // `javaFiles` are compiled into an unnamed module.
        //   (is that correct? does Javac checks dir hierarchy for module-info.java?)
        // they will reference user modules, and rekex modules.
        //
        // In the end, this method may not work at all. For example, the JVM was launched
        // with minimal classpath/module-path; some code then dynamically create a ClassLoader
        // to launch a sub-app, which reaches this method. The system properties don't
        // contain necessary classpath/module-path for the sub-app.
        // User might have to manually add a lot of stuff in `options` to make it work.
        //
        // If nothing works, ParserBuilder can't directly compile the files then load the classes.
        // user will have to be content with compiling `javaFiles` in a separate build step;

        if(!options.contains("--add-modules"))
        {
            options = new ArrayList<>(options);
            options.add("--add-modules");
            options.add("ALL-MODULE-PATH");
        }


        {
            var path0 = System.getProperty("jdk.module.path");
            var path1 = fileManager.getLocation(StandardLocation.MODULE_PATH);
            if(path1==null && path0!=null)
            {
                String[] arr = path0.split(Pattern.quote(File.pathSeparator));
                ArrayList<File> files = new ArrayList<>();
                for(String s : arr)
                    files.add(new File(s));
                fileManager.setLocation(StandardLocation.MODULE_PATH, files);
            }
        }

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
