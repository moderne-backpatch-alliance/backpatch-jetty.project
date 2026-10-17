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

package org.eclipse.jetty.server.session;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SessionListenerExceptionTest
{
    private Server server;
    private HashSessionIdManager idManager;

    @Before
    public void startServer() throws Exception
    {
        server = new Server();
        idManager = new HashSessionIdManager();
        server.setSessionIdManager(idManager);
        server.start();
    }

    @After
    public void stopServer() throws Exception
    {
        server.stop();
    }

    private HashSessionManager startManager(HttpSessionListener listener) throws Exception
    {
        SessionHandler handler = new SessionHandler();
        handler.setServer(server);
        HashSessionManager manager = new HashSessionManager();
        handler.setSessionManager(manager);
        manager.setSessionIdManager(idManager);
        if (listener != null)
            manager.addEventListener(listener);
        manager.start();
        return manager;
    }

    @Test
    public void testSessionInvalidatedWhenDestroyedListenerThrows() throws Exception
    {
        HashSessionManager manager = startManager(new ThrowingListener());
        AbstractSession session = (AbstractSession)manager.newHttpSession(new Request(null, null));

        session.invalidate();

        Assert.assertFalse(session.isValid());
        Assert.assertFalse(idManager.idInUse(session.getClusterId()));
        manager.stop();
    }

    @Test
    public void testOtherContextsInvalidatedWhenDestroyedListenerThrows() throws Exception
    {
        HashSessionManager managerA = startManager(null);
        HashSessionManager managerB = startManager(new ThrowingListener());
        HashSessionManager managerC = startManager(null);
        Request request = new Request(null, null);
        AbstractSession sessionA = (AbstractSession)managerA.newHttpSession(request);
        AbstractSession sessionB = (AbstractSession)managerB.newHttpSession(request);
        AbstractSession sessionC = (AbstractSession)managerC.newHttpSession(request);
        Assert.assertEquals(sessionA.getClusterId(), sessionB.getClusterId());
        Assert.assertEquals(sessionA.getClusterId(), sessionC.getClusterId());

        sessionA.invalidate();

        Assert.assertFalse(sessionA.isValid());
        Assert.assertFalse(sessionB.isValid());
        Assert.assertFalse(sessionC.isValid());
        managerA.stop();
        managerB.stop();
        managerC.stop();
    }

    @Test
    public void testSessionCreatedWhenCreatedListenerThrows() throws Exception
    {
        HashSessionManager manager = startManager(new ThrowingListener());

        AbstractSession session = (AbstractSession)manager.newHttpSession(new Request(null, null));

        Assert.assertTrue(session.isValid());
        Assert.assertSame(session, manager.getSession(session.getClusterId()));
        manager.stop();
    }

    public static class ThrowingListener implements HttpSessionListener
    {
        @Override
        public void sessionCreated(HttpSessionEvent se)
        {
            throw new IllegalStateException("Exception during sessionCreated");
        }

        @Override
        public void sessionDestroyed(HttpSessionEvent se)
        {
            throw new IllegalStateException("Exception during sessionDestroyed");
        }
    }
}
