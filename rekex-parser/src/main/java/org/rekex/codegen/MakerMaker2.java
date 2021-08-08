package org.rekex.codegen;

import java.io.IOException;
import java.util.ArrayList;

// this file is generated from org/rekex/codegen/MakerTemplate.java; do not modify.
class MakerMaker2
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

    public void classHeader(Object packageName, Object className, Object fromTemplateFile)
    {
        add(this._classHeader, 0, 8);
        add(packageName);
        add(this._classHeader, 21, 112);
        add(fromTemplateFile);
        add(this._classHeader, 130, 153);
        add(className);
        add(this._classHeader, 164, 859);
    }
    final String _classHeader = """
package {packageName};

import java.io.IOException;
import java.util.ArrayList;

// this file is generated from {fromTemplateFile}; do not modify.
class {className}
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

""";



    public void segmentHeader(Object segName, Object args)
    {
        add(this._segmentHeader, 0, 16);
        add(segName);
        add(this._segmentHeader, 25, 26);
        add(args);
        add(this._segmentHeader, 32, 40);
    }
    final String _segmentHeader = """
    public void {segName}({args})
    {
""";



    public void addArg(Object arg)
    {
        add(this._addArg, 0, 12);
        add(arg);
        add(this._addArg, 17, 20);
    }
    final String _addArg = """
        add({arg});
""";



    public void addBodyPart(Object segName, Object from, Object to)
    {
        add(this._addBodyPart, 0, 18);
        add(segName);
        add(this._addBodyPart, 27, 29);
        add(from);
        add(this._addBodyPart, 35, 37);
        add(to);
        add(this._addBodyPart, 41, 44);
    }
    final String _addBodyPart = """
        add(this._{segName}, {from}, {to});
""";



    public void segmentFooter(Object segName, Object body)
    {
        add(this._segmentFooter, 0, 24);
        add(segName);
        add(this._segmentFooter, 33, 40);
        add(body);
        add(this._segmentFooter, 46, 54);
    }
    final String _segmentFooter = """
    }
    final String _{segName} = \"""
{body}\""";



""";



    public void classFooter()
    {
        add(this._classFooter, 0, 2);
    }
    final String _classFooter = """
}
""";



}
