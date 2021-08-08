package org.rekex.exmple.parser.json;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.rekex.parser.ParseResult;
import org.rekex.parser.PegParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

class ExampleParser_JsonTest
{

    @Test
    void test() throws Exception
    {
        {
            // src/test/resources/json_test
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            var dirURL = loader.getResource("json_test");
            var dir = Paths.get(dirURL.toURI());
            testDir(dir);
        }
        {
            // test cases from https://github.com/nst/JSONTestSuite
            // I don't want to check them in to our git.
            var dir = Paths.get("/tmp/JSONTestSuite/test_parsing");
            if(Files.isDirectory(dir))
                testDir(dir);
            else // not fatal. I've tested them on my machine so you don't have to:)
                System.err.println("#### test dir not found ### : "+dir);
        }
    }

    List<PegParser<?>> parsers = List.of(
        ExampleParser_Json1.parser(),
        ExampleParser_Json2.parser(),
        ExampleParser_Json3.parser()
    );
    Set<String> skippedFiles = Set.of(
        // stackoverflow due to deeply nested structure
        // that's ok for us - we admit we have recursion limits.
        "n_structure_100000_opening_arrays.json",
        "n_structure_open_array_object.json"
    );

    void testDir(Path dir) throws Exception
    {
        Files.list(dir).forEach(p->testFile(p));
    }
    void testFile(Path file)
    {
        if(!Files.isRegularFile(file))
            return;
        String fileName = file.getFileName().toString();
        if(!fileName.endsWith(".json"))
            return;
        if(skippedFiles.contains(fileName))
            return;

        boolean good;
        if(fileName.startsWith("y_"))
            good = true;
        else if(fileName.startsWith("n_"))
            good = false;
        else
            return;

        //System.out.println(file);

        String content;
        try
        {
            content = Files.readString(file, StandardCharsets.UTF_8);
        }
        catch(java.nio.charset.MalformedInputException e)
        {
            if(!good) // file was made malformed to fail the test
                return;
            else
                throw new AssertionFailedError("malformed file; file="+file, e);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }

        ParseResult<?> result = null;
        for(var parser : parsers)
        {
            result = parse(parser, content);

            if(result instanceof ParseResult.Fatal<?> fatal)
                throw new AssertionFailedError("fatal error; file="+file+", result="+result+", parser="+parser, fatal.cause());

            if(!good && (result instanceof ParseResult.Full))
                throw new AssertionFailedError("expect to fail, but passed; file="+file+", result="+result+", parser="+parser);
            if(good && (!(result instanceof ParseResult.Full)))
                throw new AssertionFailedError("expect to pass, but failed; file="+file+", result="+result+", parser="+parser);
        }

        if(false && !good) // see how it failed
        {
            System.out.println("#### "+file);
            System.out.println(content);
            //System.out.println(Arrays.toString(content.getBytes(StandardCharsets.UTF_8)));
            System.out.println(result);
        }
    }

    <T> ParseResult<T> parse(PegParser<T> parser, String content)
    {
        try
        {
            return parser.parse(content);
        }
        catch (StackOverflowError error)
        {
            return new ParseResult.Fatal<>(0, new Exception("StackOverflowError"), List.of());
        }
    }
}