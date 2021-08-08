package org.rekex.codegen;

//  directives: lines starting with Ws//**
//    //** template segName(params)   -- start a template segment
//    //** end                        -- start a segment which is ignored
//    //** comment                    -- comment; ignored this line
//    //** line Content               -- as a normal line of <WsContent>

import java.nio.file.Paths;

//** template classHeader(packageName, className`MakerTemplate`, fromTemplateFile) +++++++++++++++++++++++
//** line package packageName;

import java.io.IOException;
import java.util.ArrayList;

//** line // this file is generated from fromTemplateFile; do not modify.
class MakerTemplate
{
    final ArrayList<Object> __parts__ = new ArrayList<>();
    private record SubSeq(CharSequence chars, int start, int end){}

    public void add(Object object)
    {
        __parts__.add(object);
    }

    public void add(CharSequence chars, int start, int end)
    {
        add(new SubSeq(chars, start, end));
    }

    public void writeTo(Appendable out) throws IOException
    {
        for(var obj : __parts__)
        {
            if(obj instanceof SubSeq ss)
                out.append(ss.chars, ss.start, ss.end);
            else
                out.append(String.valueOf(obj));
        }
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    //** template segmentHeader(segName, args`Object arg`) ++++++++++++++++++++++++++++++
    public void segName(Object arg)
    {
        //** template addArg(arg) +++++++++++++++++++++++++++++++++++++++++++++++++++++++
        add(arg);
        //**template addBodyPart(segName, from`1111`, to`2222`) ++++++++++++++++++++++++
        add(this._segName, 1111, 2222);
        //** template segmentFooter(segName, body) ++++++++++++++++++++++++++++++++++++++
    }
    final String _segName = """
body""";



    //** end ---------------------------------------------------------------------------

    // for rekex dev. run main() after modifying this file. MakerTemplate -> MakerMaker
    //
    // TemplateToMaker uses MakerMaker_2 (MM2) to generate target file.
    // MM2 was generated from this template file (T1) in the past.
    // Generate again,  T1 + MM2 -> MM3,  MM3 and MM2 are the same (except class name)
    //
    // If this template is modified to T2, generate again, T2+MM2->MM3,
    // MM3 is different from MM2, but still contains stuff from MM2 & T1.
    // Make TemplateToMaker depend on MM3, generate again, T2+MM3->MM4.
    // Make TemplateToMaker depend on MM4. Now MM4 is a fixed point,
    // try T2+MM4->MM5, MM5 and MM4 are the same (except class name)
    public static void main(String[] args) throws Exception
    {
        String targetClassName = "MakerMaker"+2;

        var currPath = Paths.get("").toAbsolutePath();
        var srcRootPath = currPath.resolve("rekex-parser/src/main/java");
        String srcClassName = MakerTemplate.class.getCanonicalName();

        System.out.println("current path: "+currPath);
        System.out.println("srcRoot path: "+srcRootPath);
        System.out.println("source class: "+srcClassName);
        System.out.println("target class: "+targetClassName);

        TemplateToMaker.transform(srcRootPath, srcClassName, targetClassName);
    }

//** template classFooter() ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
}
