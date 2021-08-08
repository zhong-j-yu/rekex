package org.rekex.codegen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public class TemplateToMaker
{
    public static void transform(Path srcRoot, String srcClassName, String targetClassSimpleName) throws Exception
    {
        int indexLastDot = srcClassName.lastIndexOf('.');
        String packageName = srcClassName.substring(0, indexLastDot);
        Path srcDir = srcRoot.resolve(packageName.replace('.', '/'));
        String srcClassSimpleName = srcClassName.substring(indexLastDot+1);
        Path srcFilePath = srcDir.resolve(srcClassSimpleName+".java");
        var lines = Files.readAllLines(srcFilePath);

        StringBuilder sb = new StringBuilder();
        var srcTemplateFile = srcRoot.relativize(srcFilePath).toString().replace("\\", "/");
        generate(lines, srcTemplateFile, packageName, targetClassSimpleName, sb);

        Path targetFilePath = srcDir.resolve(targetClassSimpleName+".java");
        String targetFileContent = sb.toString();
        if(!"\n".equals(System.lineSeparator()))
            targetFileContent = targetFileContent.replace("\n", System.lineSeparator());
        Files.writeString(targetFilePath, targetFileContent);
    }

    record SegmentIgnored(){}
    record LineIgnored(){}
    record Segment(String name, List<Param> params, List<String> body){}
    record Param(String name, String literal){}

    static void generate(List<String> lines, String srcTemplateFile, String targetPackageName,
                                 String targetClassSimpleName, Appendable out) throws Exception
    {
        var mm = new MakerMaker2();

        mm.classHeader(targetPackageName, targetClassSimpleName, srcTemplateFile);

        HashSet<String> segNames = new HashSet<>();
        ArrayList<Segment> segments = new ArrayList<>();
        Segment currSeg = null;
        for(var line : lines)
        {
            Object parsed = parseLine(line);
            if(parsed instanceof LineIgnored)
            {
                // ignore
            }
            else if(parsed instanceof SegmentIgnored)
            {
                currSeg = null;
            }
            else if(parsed instanceof Segment seg)
            {
                if(!segNames.add(seg.name))
                    throw new Exception("duplicate segment name: "+seg.name);
                currSeg = seg;
                segments.add(seg);
            }
            else if(parsed instanceof String str)
            {
                if(currSeg!=null)
                    currSeg.body.add(str.stripTrailing());
            }
            else
                throw new AssertionError();
        }

        for(var seg : segments)
        {
            String args = seg.params.stream()
                .map(p->"Object "+p.name)
                .collect(Collectors.joining(", "));
            mm.segmentHeader(seg.name, args);

            // it's important that there's no trailing spaces on each line.
            String body1 = String.join("\n", seg.body)+"\n";
            // replace param.literal in body1 to {param.name} in body2
            StringBuilder body2 = new StringBuilder();
            int L = body1.length();
            int N = seg.params.size();
            int[] occurrences = new int[N];
            int start=0;
            while(start<L)
            {
                // find first occurrences of a param in body[start,end)
                int paramIndex = -1;
                int pos = L;
                for(int p=0; p<N; p++)
                {
                    var param = seg.params.get(p);
                    int x = body1.indexOf(param.literal, start);
                    assert x!=pos; // no two literals can match at the same position
                    if(x!=-1 && x<pos)
                    {
                        pos = x;
                        paramIndex = p;
                    }
                }
                if(paramIndex==-1)
                {
                    int m = body2.length();
                    body2.append(body1, start, L);
                    mm.addBodyPart(seg.name, m, m+L-start);
                    start=L;
                }
                else
                {
                    occurrences[paramIndex]++;

                    int m = body2.length();
                    body2.append(body1, start, pos);
                    mm.addBodyPart(seg.name, m, m+pos-start);

                    var param = seg.params.get(paramIndex);
                    mm.addArg(param.name);

                    start = pos+param.literal.length();
                    body2.append('{').append(param.name).append('}');
                }
            }
            for(int p=0; p<N; p++)
            {
                if(occurrences[p]==0)
                {
                    var param = seg.params.get(p);
                    throw new Exception("param `%s` not encountered in segment `%s`"
                        .formatted(param.name, seg.name));
                }
            }

            var body2Esc = escapeTextBlock(body2);
            mm.segmentFooter(seg.name, body2Esc);
        }

        mm.classFooter();
        mm.writeTo(out);
    }

    // body2 is going to be put in a text block.
    // we need to escape \ in it,
    // and make sure there are no 3 consecutive """
    // note that body2 always ends with \n,
    // so it doesn't have trailing quotes that may
    // connect with the closing """ of the text block.
    static CharSequence escapeTextBlock(CharSequence body2)
    {
        final char QT='"', BS='\\';
        assert body2.length()>0
            && QT!=body2.charAt(body2.length()-1);
        StringBuilder sb = new StringBuilder();
        int quoteCount=0;
        for(int i=0; i<body2.length(); i++)
        {
            char c = body2.charAt(i);
            if(c==QT) // escape the 1st quote of 3 quotes
            {
                quoteCount++;
                if(quoteCount==3)
                    sb.insert(sb.length()-2, BS);
                else if(quoteCount>3 && quoteCount%3==1)
                    sb.append(BS);
                sb.append(QT);
            }
            else
            {
                quoteCount=0;
                if(c==BS)
                    sb.append(BS).append(BS);
                else
                    sb.append(c);
            }
        }
        return sb;
    }




    // e.g.    //** template name( param1, param2 `literal2` ) ....
    //         body...
    //         //** bypass
    //         ignored
    static Object parseLine(String str) throws Exception
    {
        try
        {
            return parseLineE(str);
        }
        catch (Exception e)
        {
            throw new Exception("error parsing line: "+str, e);
        }
    }
    static final String directivePrefix = "//**";
    static Set<String> reservedSegNames = Set.of("__parts__", "add", "writeTo");
    static Set<String> reservedParamNames = Set.of("this");
    static Object parseLineE(String chars) throws Exception
    {
        int start = 0;
        int end = chars.length();
        start = skipWs(chars, start, end);
        int leadingWs = start;

        start = matchPrefix(directivePrefix, chars, start, end);
        if(start==-1) // no prefix; a normal line
            return chars;
        start = skipWs(chars, start, end);

        int start0 = start;
        start = matchIdentifier(chars, start, end);
        if(start==-1)
            throw new Exception("directive expected");
        var dirName = chars.substring(start0, start);

        if(dirName.equals("comment"))
            return new LineIgnored();
        if(dirName.equals("end"))
            return new SegmentIgnored();
        if(dirName.equals("line"))
        {
            // ws//** line Content => wsContent
            // keep leading whitespaces; consume one whitespace after "line"
            return chars.substring(0, leadingWs) +  chars.substring(start+1);
        }
        if(!dirName.equals("template"))
            throw new Exception("unknown directive");

        start = skipWs(chars, start, end);

        start0 = start;
        start = matchIdentifier(chars, start, end);
        if(start==-1)
            throw new Exception("identifier expected");
        var segName = chars.subSequence(start0, start).toString();
        start = skipWs(chars, start, end);

        if(reservedSegNames.contains(segName))
            throw new Exception("reserved segment name: "+segName);

        start = matchPrefix("(", chars, start, end);
        if(start==-1) throw new Exception("( expected");
        start = skipWs(chars, start, end);

        ArrayList<Param> params = new ArrayList<>();
        while(true)
        {
            if(-1!=matchPrefix(")", chars, start, end)) // all done
                return new Segment(segName, params, new ArrayList<>());

            if(!params.isEmpty())
            {
                start = matchPrefix(",", chars, start, end);
                if(start==-1)
                    throw new Exception("comma expected");
                start = skipWs(chars, start, end);
            }

            start0 = start;
            start = matchIdentifier(chars, start, end);
            if(start==-1) throw new Exception("param name expected");
            String paramName = chars.subSequence(start0, start).toString();
            start = skipWs(chars, start, end);

            String paramLiteral = paramName;
            if(-1!=matchPrefix("`", chars, start, end))
            {
                start++;
                start0 = start;
                start = matchPredicate(c->c!='`', chars, start, end);
                if(start==end) throw new Exception("closing ` expected");
                paramLiteral = chars.subSequence(start0, start).toString();
                start++;
                start = skipWs(chars, start, end);
            }

            if(reservedParamNames.contains(paramName))
                throw new Exception("reserved param name: "+paramName);
            for(var param : params)
            {
                if(param.name.equals(paramName))
                    throw new Exception("duplicate param name: "+paramName);
                if(param.literal.startsWith(paramLiteral) || paramLiteral.startsWith(param.literal))
                    throw new Exception("conflicting param literals %s`%s` vs %s`%s`".formatted(
                        param.name, param.literal, paramName, paramLiteral
                    ));
            }
            params.add(new Param(paramName, paramLiteral));
        }
    }

    static int skipWs(CharSequence chars, int start, int end)
    {
        return matchPredicate(c->(c==' ' || c=='\t'), chars, start, end);
    }
    static int matchPrefix(String prefix, CharSequence chars, int start, int end)
    {
        int N = prefix.length();
        if(N > end-start)
            return -1;
        for(int i=0; i<N; i++)
            if(prefix.charAt(i)!=chars.charAt(start+i))
                return -1;
        return start+N;
    }
    static int matchPredicate(IntPredicate charPred, CharSequence chars, int start, int end)
    {
        while(start<end && charPred.test(chars.charAt(start)))
            start++;
        return start;
    }
    static int matchIdentifier(CharSequence chars, int start, int end)
    {
        int s2 = matchPredicate(Character::isJavaIdentifierPart, chars, start, end);
        if(s2==start)
            return -1;
        if(!Character.isJavaIdentifierStart(chars.charAt(start)))
            return -1;
        return s2;
    }
}
