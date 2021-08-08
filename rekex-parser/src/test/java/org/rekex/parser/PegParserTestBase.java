package org.rekex.parser;

import org.opentest4j.AssertionFailedError;
import org.rekex.annotype.AnnoType;
import org.rekex.annotype.ClassType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class PegParserTestBase
{
    protected boolean generateJavaHere = false;
    protected boolean dumpResult = false;
    protected boolean dumpGrammar = false;

    protected List<PegParser<?>> parsers;

    protected void initParsers(Class<?> rootClass, Class<?> ctorCatalog)
    {
        var type = ClassType.of(rootClass);
        initParsers(type, ctorCatalog);
    }
    protected void initParsers(AnnoType rootType, Class<?> ctorCatalog)
    {
        var builder = new PegParserBuilder()
            .logger(msg->{})
            .rootType(rootType)
            .ctorCatalog(ctorCatalog);

        if(generateJavaHere) // for dev time only
        {
            var currPath = Paths.get("").toAbsolutePath();
            // for IntelliJ, currPath = "regex"
            // for Maven, currPath = "regex/rekex-parser"
            var srcRoot = currPath.resolve("rekex-parser/src/test/java");
            if(!Files.isDirectory(srcRoot))
            {
                // this feature should only be used in IntelliJ, temporarily and manually.
                System.err.println("not a dir: "+srcRoot);
            }
            else
            {
                builder.logger(System.out::println);
                builder.outDirForJava(srcRoot);
                var thisClass = this.getClass();
                builder.packageName(thisClass.getPackageName());
                builder.className(thisClass.getSimpleName()+"__Gen");
            }
        }

        PegParser<?> parser2 = builder.parser();

        PegParser<?> parser1 = ReferencePegParser.of(rootType, ctorCatalog);

        this.parsers = List.of(parser1, parser2);

        if(dumpGrammar)
            System.out.println(builder.grammar().toText());
    }

    protected void matchFull(String input)
    {
        for(var parser : parsers)
        {
            Object value;
            try
            {
                value = parser.matchFull(input);
            }
            catch (Exception exception)
            {
                throw new RuntimeException(exception);
            }
            if(dumpResult)
                System.out.println("value="+value);
        }
    }
    protected void matchFull(String input, Object val)
    {
        for(var parser : parsers)
        {
            Object value;
            try
            {
                value = parser.matchFull(input);
            }
            catch (Exception exception)
            {
                throw new RuntimeException(exception);
            }
            if(dumpResult)
                System.out.println("value="+value);
            assertEquals(value, val);
        }
    }

    protected void matchPartial(String input, int position)
    {
        for(var parser : parsers)
        {
            var result = parser.parse(input);
            if(dumpResult)
                System.out.println("result="+result);
            if(result instanceof ParseResult.Partial<?> partial)
            {
                assertEquals(position, partial.position());
            }
            else
            {
                throw new AssertionFailedError("not Partial: " + result);
            }
        }
    }

    protected void matchFail(String input, int failPosition, int... starts)
    {
        for(var parser : parsers)
        {
            var result = parser.parse(input);
            if(dumpResult)
                System.out.println("result="+result);
            if(result instanceof ParseResult.Fail<?> fail)
            {
                checkPositions(fail, fail.position(), fail.stack(), failPosition, starts);
            }
            else
            {
                throw new AssertionFailedError("not Fail: " + result);
            }
        }
    }

    private void checkPositions(ParseResult<?> result, int resPos, List<ParseResult.Node> stack,
                                int failPosition, int[] starts)
    {
        assertEquals(failPosition, resPos, "fail pos\n"+ result);
        assertEquals(starts.length, stack.size(), "starts don't match\n"+ result);
        for(int i = 0; i< starts.length; i++)
            if(starts[i]!= stack.get(i).start())
                throw new AssertionFailedError("starts don't match\n" + result);
    }

    protected void matchFatal(String input, int failPosition, int... starts)
    {
        for(var parser : parsers)
        {
            var result = parser.parse(input);
            if(dumpResult)
                System.out.println("result="+result);
            if(result instanceof ParseResult.Fatal<?> fatal)
            {
                var ex = fatal.cause();
                if(dumpResult)
                    ex.printStackTrace();
                checkPositions(fatal, fatal.position(), fatal.stack(), failPosition, starts);
            }
            else
            {
                throw new AssertionFailedError("not Fatal: " + result);
            }
        }
    }
}