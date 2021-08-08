package org.rekex.exmple.regexp;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.rekex.regexp.RegExp;
import org.rekex.regexp.RegExpApi;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ExampleRegExp_UriTest
{
    // from https://datatracker.ietf.org/doc/html/rfc3986
    // IP address not supported as host
    public static final String[] examples = {
        "ftp://ftp.is.co.za/rfc/rfc1808.txt",
        "http://www.ietf.org/rfc/rfc2396.txt",
        //"ldap://[2001:db8::7]/c=GB?objectClass?one",
        "mailto:John.Doe@example.com",
        "news:comp.infosystems.www.servers.unix",
        "tel:+1-816-555-1212",
        //"telnet://192.0.2.16:80/",
        "urn:oasis:names:specification:docbook:dtd:xml:4.1.2",
        "foo://example.com:8042/over/there?name=ferret#nose",
        "http://a/b/c/g;x?y#s",
        "http://a/b/c/g?y/./x",
        "http://a/b/c/g?y/../x",
        "example://a/b/c/%7Bfoo%7D",
        "eXAMPLE://a/./b/../b/%63/%7bfoo%7d",
        "http://example.com",
        "http://example.com/",
        "http://example.com:/",
        "http://example.com:80/",
        "http://example.com/data",
        "http://example.com/data/",
        "ftp://cnn.example.com&story=breaking_news@10.0.0.1/top_story.htm",
        "http://www.ics.uci.edu/pub/ietf/uri/#Related",
    };

    @Test
    void test()
    {
        var exp = ExampleRegExp_Uri.exp();
        ExampleRegExpTestBase.matches(examples, exp);
    }

}