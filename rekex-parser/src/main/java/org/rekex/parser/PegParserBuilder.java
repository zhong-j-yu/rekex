package org.rekex.parser;

import org.rekex.annotype.AnnoType;
import org.rekex.annotype.ClassType;
import org.rekex.grammar.Grammar;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builder for {@link PegParser}.
 */
public class PegParserBuilder
{
    Class<?> catalogClass;
    AnnoType rootType;
    String packageName;
    String className;
    Path outDirForJava;
    Path outDirForClass;
    List<String> javacOptions;
    ClassLoader parentClassLoader;
    Consumer<CharSequence> logger;

    // derivative
    Grammar grammar;
    String grammarMd5;

    /**
     * Create a builder.
     */
    public PegParserBuilder()
    {

    }

    /**
     * Set the ctor catalog class.
     */
    public PegParserBuilder catalogClass(Class<?> catalogClass)
    {
        this.catalogClass = catalogClass;
        this.grammar=null;
        this.grammarMd5=null;
        return this;
    }
    Class<?> catalogClass()
    {
        return catalogClass; // null is ok
    }

    /**
     * Set the root type for the grammar and the parser.
     */
    public PegParserBuilder rootType(AnnoType rootType)
    {
        this.rootType = rootType;
        this.grammar=null;
        this.grammarMd5=null;
        return this;
    }
    /**
     * Set the root type for the grammar and the parser.
     */
    public PegParserBuilder rootType(Class<?> rootClass)
    {
        return rootType(ClassType.of(rootClass));
    }
    AnnoType rootType()
    {
        if(rootType==null)
            throw new IllegalStateException("rootType unspecified");
        return rootType;
    }

    /**
     * The package name of the generated parser class.
     */
    public PegParserBuilder packageName(String packageName)
    {
        this.packageName = packageName;
        return this;
    }
    String packageName()
    {
        if(packageName==null)
            packageName = "rekex_peg_parser_"+grammarMd5();
        return packageName;
    }

    /**
     * The simple name of the generated parser class.
     */
    public PegParserBuilder className(String className)
    {
        this.className = className;
        return this;
    }
    String className()
    {
        if(className==null)
            className = "PegParser_"+grammarMd5();
        return className;
    }

    /**
     * The root directory for the generated Java source file of the parser.
     * The file will be under a sub directory according to the package structure.
     * The default directory is somewhere under "/tmp".
     */
    public PegParserBuilder outDirForJava(Path outDirForJava)
    {
        this.outDirForJava = outDirForJava;
        return this;
    }
    Path outDirForJava()
    {
        if(outDirForJava==null)
            outDirForJava = Paths.get("/tmp/rekex_generated/src");
        return outDirForJava;
    }

    /**
     * The root directory for compiled class files.
     * The files will be under a sub directory according to the package structure.
     * The default directory is somewhere under "/tmp".
     */
    public PegParserBuilder outDirForClass(Path outDirForClass)
    {
        this.outDirForClass = outDirForClass;
        return this;
    }
    Path outDirForClass()
    {
        if(outDirForClass==null)
            outDirForClass = Paths.get("/tmp/rekex_generated/classes");
        return outDirForClass;
    }

    /**
     * Options for javac; default to `javacOptions("-g")`.
     */
    public PegParserBuilder javacOptions(String... javacOptions)
    {
        this.javacOptions = List.of(javacOptions);
        return this;
    }
    List<String> javacOptions()
    {
        if(javacOptions==null)
            javacOptions = List.of("-g");
        return javacOptions;
    }

    /**
     * The parent class loader for the dynamic class loader that loads
     * the generated parser class. Default to the context class loader.
     */
    public PegParserBuilder parentClassLoader(ClassLoader parentClassLoader)
    {
        this.parentClassLoader = parentClassLoader;
        return this;
    }
    ClassLoader parentClassLoader()
    {
        if(parentClassLoader==null)
        {
            // what's the proper classloader? from user datatypes?
            parentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        return parentClassLoader;
    }

    /**
     * Where logging information will be delivered to. Default to stdout.
     */
    public PegParserBuilder logger(Consumer<CharSequence> logger)
    {
        this.logger = logger;
        return this;
    }
    Consumer<CharSequence> logger()
    {
        // by default, print info to stdout, for the benefit of new users.
        if(logger==null)
            logger = System.out::println;
        return logger;
    }

    /**
     * Send the grammar in textual form to `out`
     */
    public PegParserBuilder dumpGrammar(Consumer<String> out)
    {
        out.accept(grammar().toText());
        return this;
    }


    // terminal actions -------------------------------------------------

    /**
     * The grammar derived from the rootType and the catalogClass.
     */
    public Grammar grammar()
    {
        if(grammar==null)
            grammar = Grammar.deriveFrom(List.of(rootType()), catalogClass());
        return grammar;
    }
    String grammarMd5()
    {
        if(grammarMd5==null)
        {
            try
            {
                MessageDigest md = MessageDigest.getInstance("MD5");
                // just the root datatype, which is stable during dev.
                String str = grammar().idToRule().get(0).datatype().toString(true);
                md.update(str.getBytes(StandardCharsets.UTF_8));
                byte[] bytes = md.digest(); // 16 bytes
                char[] chars = new char[bytes.length];
                for(int i=0; i<bytes.length; i++)
                    chars[i] = "0123456789ABCDEF".charAt(bytes[i]&0x0f);
                grammarMd5 = new String(chars);
            }
            catch (Exception exception)
            {
                throw new RuntimeException(exception);
            }
        }
        return grammarMd5;
    }

    /**
     * Return generated source code in String.
     */
    public String generateJavaSource()
    {
        StringBuilder sb = new StringBuilder();
        try
        {
            PegParserCodeGen.generate(rootType(), grammar(), packageName(), className(), sb);
        }
        catch (Exception exception)
        {
            throw new RuntimeException(exception);
        }
        String targetFileContent = sb.toString();
        if(!"\n".equals(System.lineSeparator()))
            targetFileContent = targetFileContent.replace("\n", System.lineSeparator());
        return targetFileContent;
    }

    /**
     * Generate the source code and save it to a file.
     */
    public Path generateJavaFile() throws Exception
    {
        Path srcDir = outDirForJava().resolve(packageName().replace('.', '/'));
        Files.createDirectories(srcDir);
        Path javaFilePath = srcDir.resolve(className()+".java");
        String targetFileContent = generateJavaSource();
        Files.writeString(javaFilePath, targetFileContent);
        return javaFilePath;
    }

    /**
     * Create an instance of the parser.
     * <p>
     *     A Java source file will be generated, compiled by javac.
     *     The parser class will be loaded, and an instance created.
     * </p>
     * <p>
     *     If a ctor catalog class is specified which contains non-static method,
     *     use {@link #build(Object) build(catalogInstance)} instead.
     * </p>
     */
    public <T> PegParser<T> build()
    {
        return build(null);
    }

    /**
     * Create an instance of the parser with `catalogInstance`
     * <p>
     *     A Java source file will be generated, compiled by javac.
     *     The parser class will be loaded, and an instance created with `catalogInstance`.
     * </p>
     */
    public <T> PegParser<T> build(Object catalogInstance)
    {
        try
        {
            return buildParserE(catalogInstance);
        }
        catch (Exception exception)
        {
            throw new RuntimeException("unable to build parser instance: ", exception);
        }
    }

    <T> PegParser<T> buildParserE(Object catalog) throws Exception
    {
        if(grammar().requiresCatalogInstance())
        {
            if(catalog==null)
                throw new Exception("must provide an instance for ctor catalog "+ catalogClass);
            if(!catalogClass().isInstance(catalog))
                throw new Exception("not an instance of ctor catalog class: "+catalog);
        }

        Path javaFilePath = generateJavaFile();

        // javac is very slow. show some message
        logger().accept("javac compiling ... "+javaFilePath.toAbsolutePath());
        PkgUtil.jc_compile(List.of(javaFilePath), outDirForClass(), javacOptions());
        logger().accept("javac done.");
        // apparently javac is slow on first invocation; but faster afterwards in the same VM.
        // so at least we don't worry too much about invoking it many times in our unittests.
        //
        // for client app, it's quite annoying to invoke javac everytime the app starts.
        // also it is not safe if there are concurrent processes sharing the out dirs.
        // before we have a clever mechanism to address the issue,
        // we advise the client to use this builder during development of the parser,
        // then merge the generated .java file to their code base, `new` the parser class directly.

        var clazz = PkgUtil.loadClass(packageName()+"."+className(), parentClassLoader(), outDirForClass());

        if(catalogClass !=null)
        {
            // the constructor requires an instance;
            // but we can pass a null if all ctors are static (don't document this "feature")
            var constructor = clazz.getConstructor(catalogClass);
            @SuppressWarnings("unchecked")
            PegParser<T> parser = (PegParser<T>)constructor.newInstance(catalog);
            return parser;
        }
        else
        {
            var constructor = clazz.getConstructor();
            @SuppressWarnings("unchecked")
            PegParser<T> parser = (PegParser<T>)constructor.newInstance();
            return parser;
        }
    }
}
