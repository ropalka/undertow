/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.undertow.protocols.http2;

import java.net.Socket;
import java.net.URISyntaxException;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.protocol.http2.Http2ServerConnection;
import io.undertow.server.protocol.http2.Http2UpgradeHandler;
import io.undertow.testutils.DefaultServer;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.xnio.Options;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractHttp2TestCase {

    static Undertow server;
    private static final int SERVER_PORT = DefaultServer.getHostPort("default") + 1;
    private static final String SERVER_HOST = DefaultServer.getHostAddress("default");

    @BeforeClass
    public static void setup() throws URISyntaxException {
        server = Undertow.builder()
                .addHttpListener(SERVER_PORT, SERVER_HOST)
                .setServerOption(UndertowOptions.ENABLE_HTTP2, true)
                .setSocketOption(Options.REUSE_ADDRESSES, true)
                .setHandler(Handlers.header(new Http2UpgradeHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        if (!(exchange.getConnection() instanceof Http2ServerConnection)) {
                            throw new RuntimeException("Not HTTP2");
                        }
                        exchange.getResponseHeaders().add(new HttpString("X-Custom-Header"), "foo");
                        // exchange.getResponseSender().send(message); // TODO: REVISIT this whole handler
                    }
                }, "h2c", "h2c-17"), Headers.SEC_WEB_SOCKET_ACCEPT_STRING, "fake")) //work around Netty bug, it assumes that every upgrade request that does not have this header is an old style websocket upgrade
                .build();

        server.start();
    }

    @AfterClass
    public static void stop() {
        server.stop();
    }

    public static Socket newClientSocket() throws Exception {
        return new Socket(SERVER_HOST, SERVER_PORT);
    }

    public static String getServerHost() {
        return SERVER_HOST;
    }

    public static int getServerPort() {
        return SERVER_PORT;
    }

}
