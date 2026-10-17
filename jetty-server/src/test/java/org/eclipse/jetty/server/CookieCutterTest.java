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

package org.eclipse.jetty.server;

import javax.servlet.http.Cookie;

import org.junit.Assert;
import org.junit.Test;

public class CookieCutterTest
{
    private static String parse(String header)
    {
        CookieCutter cutter = new CookieCutter();
        cutter.addCookieField(header);
        StringBuilder result = new StringBuilder();
        for (Cookie cookie : cutter.getCookies())
        {
            if (result.length() > 0)
                result.append(" | ");
            result.append(cookie.getName()).append('=').append(cookie.getValue());
        }
        return result.toString();
    }

    @Test
    public void testSemicolonInQuotedValueDropsCookie()
    {
        Assert.assertEquals("C=3", parse("A=\"1; B=2\"; C=3"));
        Assert.assertEquals("", parse("A=\"1; B=2; C=3"));
        Assert.assertEquals("", parse("DISPLAY_LANGUAGE=\"b; JSESSIONID=1337; c=d\""));
        Assert.assertEquals("c=d", parse("DISPLAY_LANGUAGE=\"b; JSESSIONID=1337\"; c=d"));
        Assert.assertEquals("B=2", parse("A=\"1\\; X=9\"; B=2"));
        Assert.assertEquals("name=value", parse("name=value; other=\"quoted=;value\""));
    }

    @Test
    public void testQuotedValues()
    {
        Assert.assertEquals("A=1 | B=2 | C=3", parse("A=1; B=2; C=3"));
        Assert.assertEquals("A=1 | B=2 | C=3", parse("A=\"1\"; B=2; C=3"));
        Assert.assertEquals("A=1 | B=2 | C=3", parse("A=\"1\"; B=\"2\"; C=\"3\""));
        Assert.assertEquals("A=1 B=2 | C=3", parse("A=\"1 B=2\"; C=3"));
        Assert.assertEquals("A=quoted=value | B=2", parse("A=\"quoted=value\"; B=2"));
    }
}
