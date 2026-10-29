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

package org.eclipse.jetty.http2;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PingFrame;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.generator.Generator;
import org.eclipse.jetty.http2.parser.Parser;
import org.eclipse.jetty.io.ByteArrayEndPoint;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HTTP2FlusherTest
{
    @Test
    public void testTerminateWithoutFailureKeepsFlusherTerminated() throws Exception
    {
        ByteBufferPool byteBufferPool = new MappedByteBufferPool();
        HTTP2Session session = new HTTP2Session(new ScheduledExecutorScheduler(), new ByteArrayEndPoint(), new Parser(byteBufferPool, 8192),
            new Generator(byteBufferPool), new Session.Listener.Adapter(), new SimpleFlowControlStrategy(), 1)
        {
            @Override
            public void onHeaders(HeadersFrame frame)
            {
            }

            @Override
            public void onPushPromise(PushPromiseFrame frame)
            {
            }
        };
        HTTP2Flusher flusher = new HTTP2Flusher(session);

        // A failed write terminates the flusher; then the session's zero streams action
        // terminates it again with its own failure, which is null after a clean GOAWAY.
        ClosedChannelException failure = new ClosedChannelException();
        flusher.terminate(failure);
        flusher.terminate(null);

        FutureCallback callback = new FutureCallback();
        HTTP2Flusher.Entry entry = new HTTP2Flusher.Entry(new PingFrame(false), null, callback)
        {
            @Override
            public int getFrameBytesGenerated()
            {
                return 0;
            }

            @Override
            protected boolean generate(ByteBufferPool.Lease lease)
            {
                return false;
            }

            @Override
            public long onFlushed(long bytes)
            {
                return 0;
            }
        };

        assertFalse(flusher.append(entry));
        assertTrue(callback.isDone());
        ExecutionException x = assertThrows(ExecutionException.class, () -> callback.get(5, TimeUnit.SECONDS));
        assertTrue(x.getCause() instanceof ClosedChannelException, String.valueOf(x.getCause()));
    }
}
