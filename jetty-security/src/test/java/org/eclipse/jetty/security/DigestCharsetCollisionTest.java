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

package org.eclipse.jetty.security;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.LocalConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DigestCharsetCollisionTest
{
    private static final String TEST_REALM = "TestRealm";
    private static final String CNONCE = "1234567890";
    private static final String URI = "/ctx/auth/info";

    // Escapes, so the test does not depend on the source encoding.
    private static final String UNICODE_PASSWORD = "pw\u4e2d\u6587123\u2605";

    private static final String COLLISION_PASSWORD = "pw??123?";

    private Server _server;
    private LocalConnector _connector;
    private ConstraintSecurityHandler _security;

    @Before
    public void setupServer() throws Exception
    {
        _server = new Server();
        _connector = new LocalConnector(_server);
        _server.setConnectors(new Connector[]{_connector});

        HashLoginService loginService = new HashLoginService(TEST_REALM);
        loginService.putUser("unicodeUser", new Password(UNICODE_PASSWORD), new String[]{"user"});
        loginService.putUser("asciiUser", new Password("password"), new String[]{"user"});
        _server.addBean(loginService);

        ContextHandler contextHandler = new ContextHandler();
        contextHandler.setContextPath("/ctx");
        _server.setHandler(contextHandler);

        SessionHandler sessionHandler = new SessionHandler();
        contextHandler.setHandler(sessionHandler);

        Constraint constraint = new Constraint();
        constraint.setAuthenticate(true);
        constraint.setName("auth");
        constraint.setRoles(new String[]{"user"});
        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/auth/*");
        mapping.setConstraint(constraint);

        _security = new ConstraintSecurityHandler();
        sessionHandler.setHandler(_security);
        _security.setHandler(new OkHandler());
        Set<String> roles = new HashSet<>();
        roles.add("user");
        _security.setConstraintMappings(Collections.singletonList(mapping), roles);
        _security.setAuthenticator(new DigestAuthenticator());

        _server.start();
    }

    @After
    public void stopServer() throws Exception
    {
        _server.stop();
    }

    @Test
    public void collisionPasswordIsRejectedForANonLatin1Password() throws Exception
    {
        String nonce = challenge();
        assertThat(get(nonce, "unicodeUser", COLLISION_PASSWORD, "1", UTF_8), startsWith("HTTP/1.1 401 Unauthorized"));
        // Sent again as ISO-8859-1, which is byte-identical here because the collision is ASCII,
        // so this rules out the rejection being an artifact of the client's charset.
        assertThat(get(nonce, "unicodeUser", COLLISION_PASSWORD, "2", ISO_8859_1), startsWith("HTTP/1.1 401 Unauthorized"));
    }

    @Test
    public void theRealNonLatin1PasswordStillAuthenticates() throws Exception
    {
        String nonce = challenge();
        assertThat(get(nonce, "unicodeUser", UNICODE_PASSWORD, "1", UTF_8), startsWith("HTTP/1.1 200 OK"));
    }

    @Test
    public void asciiPasswordAuthenticationIsUnaffected() throws Exception
    {
        String nonce = challenge();
        assertThat(get(nonce, "asciiUser", "password", "1", UTF_8), startsWith("HTTP/1.1 200 OK"));
        assertThat(get(nonce, "asciiUser", "WRONG", "2", UTF_8), startsWith("HTTP/1.1 401 Unauthorized"));
    }

    @Test
    public void challengeAdvertisesTheUtf8Charset() throws Exception
    {
        String response = _connector.getResponses("GET " + URI + " HTTP/1.0\r\n\r\n");
        assertThat(response, startsWith("HTTP/1.1 401 Unauthorized"));
        assertThat(response, containsString("charset=UTF-8"));
    }

    private String challenge() throws Exception
    {
        String response = _connector.getResponses("GET " + URI + " HTTP/1.0\r\n\r\n");
        assertThat(response, startsWith("HTTP/1.1 401 Unauthorized"));
        Matcher matcher = Pattern.compile("nonce=\"([^\"]*)\",").matcher(response);
        assertTrue("no nonce in the challenge", matcher.find());
        return matcher.group(1);
    }

    private String get(String nonce, String username, String password, String nc, Charset charset) throws Exception
    {
        String response = digest(nonce, username, password, nc, charset);
        return _connector.getResponses("GET " + URI + " HTTP/1.0\r\n" +
            "Authorization: Digest username=\"" + username + "\", qop=auth, cnonce=\"" + CNONCE + "\", " +
            "uri=\"" + URI + "\", realm=\"" + TEST_REALM + "\", " +
            "nc=" + nc + ", " +
            "nonce=\"" + nonce + "\", " +
            "response=\"" + response + "\"\r\n" +
            "\r\n");
    }

    private String digest(String nonce, String username, String password, String nc, Charset charset) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String a1 = username + ":" + TEST_REALM + ":" + password;
        byte[] ha1 = md.digest(a1.getBytes(charset));
        String a2 = "GET:" + URI;
        byte[] ha2 = md.digest(a2.getBytes(charset));
        String expected = TypeUtil.toString(ha1, 16) + ":" + nonce + ":" + nc + ":" + CNONCE + ":auth:" +
            TypeUtil.toString(ha2, 16);
        return TypeUtil.toString(md.digest(expected.getBytes(charset)), 16);
    }

    private static class OkHandler extends AbstractHandler
    {
        @Override
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException
        {
            baseRequest.setHandled(true);
            response.setStatus(200);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().println("user=" + request.getRemoteUser());
        }
    }
}
