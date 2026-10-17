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
            "https://user@host@other.com/",
            "https://[notIpv6]/",
            "https://bad[0::1::2::3::4]/",
            "http://browser.check &@vulndetector.com/",
            "http://browser.check #@vulndetector.com/",
            "http://[normal.com@]vulndetector.com/",
            "http://normal.com[user@vulndetector].com/",
            "http://normal.com[@]vulndetector.com/",
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
            {"http://[::]/path","[::]","-1"},
            {"http://[0:0:0:0:0:ffff:127.0.0.1]/path","[0:0:0:0:0:ffff:127.0.0.1]","-1"},
            {"http://[1:2:3:4:5:6:7:8]:80/path","[1:2:3:4:5:6:7:8]","80"},
            {"http://[1::]/path","[1::]","-1"},
            {"http://[::1]","[::1]","-1"},
        };
        for (String[] test:good)
        {
            HttpURI uri = new HttpURI(test[0]);
            assertEquals(test[0], test[1], uri.getHost());
            assertEquals(test[0], Integer.parseInt(test[2]), uri.getPort());
        }
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testAuthorityEndsAtQueryOrFragment() throws Exception
    {
        HttpURI uri = new HttpURI("http://good.com#@evil.com");
        assertEquals("good.com", uri.getHost());
        assertEquals("//good.com", uri.getAuthority());
        assertEquals(null, uri.getPath());
        assertEquals(null, uri.getQuery());
        assertEquals("@evil.com", uri.getFragment());

        uri = new HttpURI("http://good.com?@evil.com");
        assertEquals("good.com", uri.getHost());
        assertEquals(null, uri.getPath());
        assertEquals("@evil.com", uri.getQuery());
        assertEquals(null, uri.getFragment());

        uri = new HttpURI("http://browser.check?@vulndetector.com/");
        assertEquals("browser.check", uri.getHost());
        assertEquals("@vulndetector.com/", uri.getQuery());
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testBadSchemesAndIPv6() throws Exception
    {
        String[] bad=
        {
            "://host/path",
            "\t://host/path",
            "  ://host/path",
            "unknown^://host/path",
            "http^://host/path",
            "https>://vulndetector.com/path",
            "1http://host/path",
            "http://[::1]x/path",
            "http://[::1]@host/path",
            "http://[1:2:3]/path",
            "http://[0::1::2]/path",
            "http://[12345::1]/path",
            "http://[1:2:3:4:5:6:7:8:9]/path",
            "http://[::256.1.1.1]/path",
            "http://[::1.2.3]/path",
            "http://[::01.2.3.4]/path",
            "http://[1.2.3.4]/path",
            "http://[]/path",
        };
        for (String uri:bad)
        {
            try
            {
                HttpURI httpURI = new HttpURI(uri);
                fail(uri+" parsed with scheme "+httpURI.getScheme()+" host "+httpURI.getHost());
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
        }
        assertEquals("svn+ssh", new HttpURI("svn+ssh://host/path").getScheme());
        assertEquals("xxxxx", new HttpURI("xxxxx://host/path").getScheme());
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testConnectSemicolonIsBadAuthority() throws Exception
    {
        for (String target:new String[]{"localhost;:443","host;x:443","[::1];:443"})
        {
            try
            {
                HttpURI uri = new HttpURI();
                uri.parseConnect(target);
                fail(target+" parsed with host "+uri.getHost());
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
        }
        HttpURI uri = new HttpURI();
        uri.parseConnect("localhost:443");
        assertEquals("localhost", uri.getHost());
        assertEquals(443, uri.getPort());
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testConnectBadAuthority() throws Exception
    {
        String[] bad=
        {
            "a@b:443",
            "user@host:443",
            "a:b@c:443",
            "[zz]:443",
            "[::1%25eth0]:443",
            "host[::1]:443",
            "h st:443",
            "h\000st:443",
            "host/path:443",
            "host?q:443",
            "host#f:443",
            "h%GGst:443",
            "host%:443",
            "host%0:443",
            "host\u00e9.com:443",
            "host:44x",
        };
        for (String target:bad)
        {
            try
            {
                HttpURI uri = new HttpURI();
                uri.parseConnect(target);
                fail(target+" parsed with host "+uri.getHost()+" port "+uri.getPort());
            }
            catch (IllegalArgumentException e)
            {
                // expected
            }
        }
    }

    /* ------------------------------------------------------------ */
    @Test
    public void testConnectGoodAuthority() throws Exception
    {
        String[][] good=
        {
            {"host:443","host","443"},
            {"[::1]:443","[::1]","443"},
            {"192.168.0.1:8080","192.168.0.1","8080"},
            {"ho%41st:443","ho%41st","443"},
            {"a-b.c_d~e!$&'()*+,=f:443","a-b.c_d~e!$&'()*+,=f","443"},
            {"[::ffff:127.0.0.1]:443","[::ffff:127.0.0.1]","443"},
        };
        for (String[] test:good)
        {
            HttpURI uri = new HttpURI();
            uri.parseConnect(test[0]);
            assertEquals(test[0], test[1], uri.getHost());
            assertEquals(test[0], Integer.parseInt(test[2]), uri.getPort());
            assertEquals(test[0], test[0], uri.getPath());
        }
    }
}
