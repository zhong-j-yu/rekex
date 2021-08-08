package org.rekex.common_util;

/**
 * Escape chars as they would appear in Java source code.
 */
public class EscapeJavaChar
{
    public static void append(StringBuilder sb, String s)
    {
        // note that we are operating on `char` level, not code points
        // a surrogate pair will be escaped into two \\uhhhh 

        for(int i=0; i<s.length(); i++)
            append(sb, s.charAt(i));
    }
    public static void appendQ(StringBuilder sb, String s)
    {
        sb.append('"');
        append(sb, s);
        sb.append('"');
    }

    public static void append(StringBuilder sb, char ch)
    {
        int escI = "\b\t\n\f\r\"'\\".indexOf(ch);
        if(escI!=-1)
            sb.append('\\').append("btnfr\"'\\".charAt(escI));
        else if(0x20<=ch&&ch<=0x7E)
            sb.append(ch);
        else
            appendU4(sb, ch);
    }
    public static void appendQ(StringBuilder sb, char ch)
    {
        sb.append('\'');
        append(sb, ch);
        sb.append('\'');
    }

    public static void appendU4(StringBuilder sb, char ch)
    {
        sb.append('\\').append('u');
        sb.append(hex(ch>>12));
        sb.append(hex(ch>>8));
        sb.append(hex(ch>>4));
        sb.append(hex(ch));
    }
    static char hex(int x)
    {
        return "0123456789ABCDEF".charAt(x & 0xF);
    }

    public static String asInJavaSource(String str)
    {
        StringBuilder sb = new StringBuilder();
        appendQ(sb, str);
        return sb.toString();
    }
}
