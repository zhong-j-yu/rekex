package org.rekex.example_modular;

import org.rekex.helper.anno.Ch;
import org.rekex.helper.anno.Str;
import org.rekex.helper.datatype.Opt;
import org.rekex.parser.PegParser;
import org.rekex.parser.PegParserBuilder;

import java.util.List;

public interface ExampleParser_Modular
{
    // binary = sign? bit*
    //   sign = "+" | "-"
    //    bit = "0" | "1"

    record Binary(Opt<Sign> optSign, List<Bit> bits){}

    enum Sign{
        @Str("+") plus,
        @Str("-") minus,
    }
    enum Bit{
        @Ch("0") v0,
        @Ch("1") v1,
    }

    static void test() throws Exception
    {
        // javac with modules is more complicated;
        // more options may be necessary to make it compile.

        PegParser<Binary> parser = new PegParserBuilder()
            .rootType(Binary.class)
            .javacOptions("-g") // default
            .build();

        Binary binary = parser.matchFull("+0101");

        if( ! binary.bits().get(3).equals(Bit.v1) )
            throw new AssertionError();
    }

    public static void main(String[] args) throws Exception
    {
        test();
    }

}
