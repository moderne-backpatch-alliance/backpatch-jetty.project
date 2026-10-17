//
//  ========================================================================
//  Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//


package org.eclipse.jetty.http;

import java.net.URI;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/* ------------------------------------------------------------ */
public class HttpURITest
{
    String[][] tests=
    {
        {"/path/to/context",null,null,"-1","/path/to/context",null,null,null},
        {"http://example.com/path/to/context;param?query=%22value%22#fragment","http","example.com","-1","/path/to/context","param","query=%22value%22","fragment"},
        {"http://[::1]/path/to/context;param?query=%22value%22#fragment","http","[::1]","-1","/path/to/context","param","query=%22value%22","fragment"},
        {"http://example.com:8080/path/to/context;param?query=%22value%22#fragment","http","example.com","8080","/path/to/context","param","query=%22value%22","fragment"},
        {"http://[::1]:8080/path/to/context;param?query=%22value%22#fragment","http","[::1]","8080","/path/to/context","param","query=%22value%22","fragment"},
    };
    
    public static int
    INPUT=0,SCHEME=1,HOST=2,PORT=3,PATH=4,PARAM=5,QUERY=6,FRAGMENT=7;

    /* ------------------------------------------------------------ */
    @Test
    public void testFromString() throws Exception
    {
        for (String[] test:tests)
        {
            HttpURI uri = new HttpURI(test[INPUT]);

            assertEquals(test[SCHEME], uri.getScheme());
            assertEquals(test[HOST], uri.getHost());
            assertEquals(Integer.parseInt(test[PORT]), uri.getPort());
            assertEquals(test[PATH], uri.getPath());
            assertEquals(test[PARAM], uri.getParam());
            assertEquals(test[QUERY], uri.getQuery());
            assertEquals(test[FRAGMENT], uri.getFragment());
        }
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testFromURI() throws Exception
    {
        for (String[] test:tests)
        {
            HttpURI uri = new HttpURI(new URI(test[INPUT]));

            assertEquals(test[SCHEME], uri.getScheme());
            assertEquals(test[HOST], uri.getHost());
            assertEquals(Integer.parseInt(test[PORT]), uri.getPort());
            assertEquals(test[PATH], uri.getPath());
            assertEquals(test[PARAM], uri.getParam());
            assertEquals(test[QUERY], uri.getQuery());
            assertEquals(test[FRAGMENT], uri.getFragment());
        }
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testSemicolonEndsAuthority() throws Exception
    {
        String[][] semicolons=
        {
            {"http://localhost;/path","http","localhost","-1",null,"/path",null,null},
            {"http://user@host;p=v/x?q#f","http","host","-1",null,"p=v/x","q","f"},
            {"http://[::1];/path","http","[::1]","-1",null,"/path",null,null},
        };
        for (String[] test:semicolons)
        {
            HttpURI uri = new HttpURI(test[INPUT]);

            assertEquals(test[INPUT], test[SCHEME], uri.getScheme());
            assertEquals(test[INPUT], test[HOST], uri.getHost());
            assertEquals(test[INPUT], Integer.parseInt(test[PORT]), uri.getPort());
            assertEquals(test[INPUT], test[PATH], uri.getPath());
            assertEquals(test[INPUT], test[PARAM], uri.getParam());
            assertEquals(test[INPUT], test[QUERY], uri.getQuery());
            assertEquals(test[INPUT], test[FRAGMENT], uri.getFragment());
        }
        assertEquals("//localhost", new HttpURI("http://localhost;/path").getAuthority());
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testBadAuthority() throws Exception
    {
        String[] bad=
        {
            "http://#host/path",
            "https:// host/path",
            "https://h st/path",
            "https://h\000st/path",
            "https://h%GGst/path",
            "https://host%/path",
            "https://host%0/path",
            "https://host%",
            "https://host%u001f/path",
            "https://host%:8080/path",
            "https://host%0:8080/path",
            "https://user%@host/path",
            "https://user%0@host/path",
            "https://host:notport/path",
            "https://user @host.com/",
            "https://user#@host.com/",
            "https://user@host@other.com/",
            "https://[notIpv6]/",
            "https://bad[0::1::2::3::4]/",
            "http://browser.check &@vulndetector.com/",
            "http://browser.check #@vulndetector.com/",
            "http://browser.check#@vulndetector.com/",
            "http://vulndetector.com\\/",
            "http://host\u00e9.com/",
        };
        for (String uri:bad)
        {
            try
            {
                HttpURI httpURI = new HttpURI(uri);
                fail(uri+" parsed with host "+httpURI.getHost());
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
        }
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testGoodAuthority() throws Exception
    {
        String[][] good=
        {
            {"http://user@host:8080/path","host","8080"},
            {"http://ho%41st/path","ho%41st","-1"},
            {"http://a-b.c_d~e!$&'()*+,=f/path","a-b.c_d~e!$&'()*+,=f","-1"},
            {"http://[::1]:8080/path","[::1]","8080"},
            {"http://user@[2001:db8::1]:8080/path","[2001:db8::1]","8080"},
            {"http://[::ffff:127.0.0.1]/path","[::ffff:127.0.0.1]","-1"},
            {"http://host","host","-1"},
        };
        for (String[] test:good)
        {
            HttpURI uri = new HttpURI(test[0]);
            assertEquals(test[0], test[1], uri.getHost());
            assertEquals(test[0], Integer.parseInt(test[2]), uri.getPort());
        }
    }
}
