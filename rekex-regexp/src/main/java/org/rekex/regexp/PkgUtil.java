package org.rekex.regexp;

import java.util.regex.Pattern;

class PkgUtil
{

    // escaped form for 00-FF
    static final String[] ESC;
    static{
        ESC = new String[0xFF + 1];

        // fallback \\uhhhh for any char. although \\xhh is shorter,
        // \\uhhhh is consistent with Java; more familiar to devs.
        // and, these control chars are rarely used anyway.
        for(char ch=0; ch<=0xFF; ch++)
            ESC[ch] = escHex4(ch);

        // some invisible chars with given names
        ESC['\t'] = "\\t";
        ESC['\n'] = "\\n";
        ESC['\r'] = "\\r";
        ESC['\f'] = "\\f";
        ESC['\u0007'] = "\\a";
        ESC['\u001B'] = "\\e";

        // SPACE is generally ok in regex (except with comment flag on)
        // We'll backslash it anyway to make it more obvious.
        ESC[' '] = "\\ ";

        // fallback for visible chars.
        for(char ch=0x21; ch<=0x7E; ch++)
            ESC[ch] = ""+ch;

        // ops with special meanings. always backslash them.
        for(char op : "\\[]^-&.$?*+{}|()".toCharArray())
            ESC[op] = "\\"+op;
    }
    static char h(int x)
    {
        return "0123456789ABCDEF".charAt(x & 0xF);
    }
    static String escHex4(char ch)
    {
        return "\\u" + h(ch>>12) + h(ch>>8) + h(ch>>4) + h(ch);
    }
    public static String esc(int ch)
    {
        // assume ch already validated
        if(ch < ESC.length)
            return ESC[ch];
        else if(ch <= 0xFFFF)
            return escHex4((char)ch);
        else
            return "\\x{" + Integer.toHexString(ch).toUpperCase() + "}";
    }




    static void validateRegex(String regex)
    {
        Pattern.compile(regex); // throws
    }


    static boolean isValidGroupName(String name)
    {
        if(name==null || name.isEmpty())
            return false;
        if(!isLetter(name.charAt(0)))
            return false;
        return name.chars()
            .allMatch(ch->isLetter(ch)||isDigit(ch));
    }
    static boolean isLetter(int ch)
    {
        return 'A'<=ch && ch<='Z'
            || 'a'<=ch && ch<='z';
    }
    static boolean isDigit(int ch)
    {
        return '0'<=ch && ch<='9';
    }








    static final int[][] flagMap = {
        {'i', Pattern.CASE_INSENSITIVE},
        {'d', Pattern.UNIX_LINES},
        {'m', Pattern.MULTILINE},
        {'s', Pattern.DOTALL},
        {'u', Pattern.UNICODE_CASE},
        {'x', Pattern.COMMENTS},
        {'U', Pattern.UNICODE_CHARACTER_CLASS},
    };
    static String flagBitsToStr(int flags)
    {
        String s = "";
        for(int[] char_int : flagMap)
        {
            char c = (char)char_int[0];
            int i = char_int[1];
            if((flags & i)!=0) // test the bit
            {
                s += c;
                flags ^= i;    // clear the bit
            }
        }
        if(flags!=0)
            throw new IllegalArgumentException("unexpected flags: "+Integer.toBinaryString(flags));
        return s;
    }


}
