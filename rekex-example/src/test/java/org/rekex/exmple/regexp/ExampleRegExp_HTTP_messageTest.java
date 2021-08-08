package org.rekex.exmple.regexp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExampleRegExp_HTTP_messageTest
{
    @Test
    void test()
    {
        // from https://datatracker.ietf.org/doc/html/rfc7230
        String[] examples = {
            """
GET /hello.txt HTTP/1.1
User-Agent: curl/7.16.3 libcurl/7.16.3 OpenSSL/0.9.7l zlib/1.2.3
Host: www.example.com
Accept-Language: en, mi
     
""",
            """
HTTP/1.1 200 OK
Date: Mon, 27 Jul 2009 12:28:53 GMT
Server: Apache
Last-Modified: Wed, 22 Jul 2009 19:15:56 GMT
ETag: "34aa387-d-1568eb00"
Accept-Ranges: bytes
Content-Length: 51
Vary: Accept-Encoding
Content-Type: text/plain

Hello World! My payload includes a trailing CRLF.
     
""",
            """
OPTIONS http://www.example.org:8001 HTTP/1.1

""",
            """
OPTIONS * HTTP/1.1
Host: www.example.org:8001

""",
        };


        for(int i=0; i<examples.length; i++)
            examples[i] = examples[i].replace("\n", "\r\n");

        var exp = ExampleRegExp_HTTP_message.exp();
        ExampleRegExpTestBase.matches(examples, exp);
    }
}