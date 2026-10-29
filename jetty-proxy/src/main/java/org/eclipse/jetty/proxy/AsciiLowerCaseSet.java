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

package org.eclipse.jetty.proxy;

import java.util.HashSet;

import org.eclipse.jetty.util.StringUtil;

// Jetty 12's org.eclipse.jetty.util.AsciiLowerCaseSet, kept in this module so the host lists' fix changes only jetty-proxy.
class AsciiLowerCaseSet extends HashSet<String>
{
    @Override
    public boolean add(String s)
    {
        return super.add(s == null ? null : StringUtil.asciiToLowerCase(s));
    }

    @Override
    public boolean contains(Object o)
    {
        if (o instanceof String)
            return super.contains(StringUtil.asciiToLowerCase((String)o));
        return super.contains(o);
    }
}
