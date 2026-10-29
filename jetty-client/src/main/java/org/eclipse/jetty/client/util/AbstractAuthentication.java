//
//  ========================================================================
//  Copyright (c) 1995-2022 Mort Bay Consulting Pty Ltd and others.
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

package org.eclipse.jetty.client.util;

import java.net.URI;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Authentication;
import org.eclipse.jetty.util.URIUtil;

public abstract class AbstractAuthentication implements Authentication
{
    private final URI uri;
    private final String realm;

    public AbstractAuthentication(URI uri, String realm)
    {
        this.uri = uri;
        this.realm = realm;
    }

    public abstract String getType();

    public URI getURI()
    {
        return uri;
    }

    public String getRealm()
    {
        return realm;
    }

    @Override
    public boolean matches(String type, URI uri, String realm)
    {
        if (!getType().equalsIgnoreCase(type))
            return false;

        if (!this.realm.equals(ANY_REALM) && !this.realm.equals(realm))
            return false;

        return matchesURI(this.uri, uri);
    }

    public static boolean matchesURI(URI uri1, URI uri2)
    {
        String scheme = uri1.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase(uri2.getScheme()))
            return false;

        String host = uri1.getHost();
        if (host == null || !host.equalsIgnoreCase(uri2.getHost()))
            return false;

        // Handle default HTTP ports.
        if (HttpClient.normalizePort(scheme, uri1.getPort()) != HttpClient.normalizePort(scheme, uri2.getPort()))
            return false;

        // Compare canonical paths.
        String path1 = canonicalPath(uri1.getRawPath());
        String path2 = canonicalPath(uri2.getRawPath());
        if (path1 == null || path2 == null)
            return false;

        if (path1.endsWith("/"))
            path1 = path1.substring(0, path1.length() - 1);

        if (path1.isEmpty())
            return true;

        if (!path2.startsWith(path1))
            return false;

        if (path2.length() == path1.length())
            return true;

        // Must match at a segment boundary.
        return path2.charAt(path1.length()) == '/';
    }

    // Stands in for Jetty 12's URIUtil.canonicalPath(rawPath), which 9.4 lacks: path parameters stripped, encoded ASCII
    // characters of its safe set decoded, other encodings kept in upper case (so %2F stays apart from '/'), dot segments removed.
    private static String canonicalPath(String rawPath)
    {
        if (rawPath == null)
            return null;
        StringBuilder builder = new StringBuilder(rawPath.length());
        boolean parameter = false;
        for (int i = 0; i < rawPath.length(); i++)
        {
            char c = rawPath.charAt(i);
            if (c == '/')
            {
                parameter = false;
                builder.append(c);
            }
            else if (parameter)
            {
                continue;
            }
            else if (c == ';')
            {
                parameter = true;
            }
            else if (c == '%')
            {
                if (i + 2 >= rawPath.length())
                    return null;
                int hi = Character.digit(rawPath.charAt(i + 1), 16);
                int lo = Character.digit(rawPath.charAt(i + 2), 16);
                if (hi < 0 || lo < 0)
                    return null;
                char decoded = (char)(hi * 16 + lo);
                if (isSafe(decoded))
                    builder.append(decoded);
                else
                    builder.append('%').append(Character.toUpperCase(rawPath.charAt(i + 1))).append(Character.toUpperCase(rawPath.charAt(i + 2)));
                i += 2;
            }
            else
            {
                builder.append(c);
            }
        }
        return URIUtil.canonicalPath(builder.toString());
    }

    // The ASCII part of Jetty 12's URIUtil.URI_SUPPORTED_CHARACTERS: RFC 3986 2.3 unreserved plus ! $ & ' ( ) * + , : = @.
    private static boolean isSafe(char c)
    {
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
            return true;
        return "-._~!$&'()*+,:=@".indexOf(c) >= 0;
    }
}
