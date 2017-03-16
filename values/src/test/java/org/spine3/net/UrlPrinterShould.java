/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.net;

import org.junit.Test;
import org.spine3.net.Url.Record;
import org.spine3.net.Url.Record.Authorization;

import static org.junit.Assert.assertEquals;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
public class UrlPrinterShould {

    private static final Authorization AUTH =
            Authorization.newBuilder()
                         .setUserName("admin")
                         .setPassword("root")
                         .build();

    private static final String HOST = "spine3.org";

    private static final Record FULL_RECORD =
            Record.newBuilder()
                  .setHost(HOST)
                  .setPort("80")
                  .setProtocol(Record.Protocol.newBuilder()
                                              .setSchema(Record.Schema.HTTP))
                  .setAuth(AUTH)
                  .setPath("index")
                  .addQuery(UrlQueryParameters.parse("key=value"))
                  .addQuery(UrlQueryParameters.parse("key2=value2"))
                  .setFragment("frag1")
                  .build();

    @Test
    public void print_valid_url() {
        final Url.Builder url = Url.newBuilder();
        url.setRecord(FULL_RECORD);

        assertEquals("http://admin:root@spine3.org:80/index?key=value&key2=value2#frag1",
                     UrlPrinter.printToString(url.build()));
    }

    @Test
    public void print_raw_url() {
        final Url.Builder url = Url.newBuilder();

        url.setRaw(HOST);

        assertEquals(HOST, UrlPrinter.printToString(url.build()));
    }

    @Test
    public void print_empty_url() {
        final Url.Builder url = Url.newBuilder();

        url.setRecord(Record.newBuilder()
                            .setHost(HOST)
                            .build());

        assertEquals(HOST, UrlPrinter.printToString(url.build()));
    }

    @Test
    public void print_url_without_password() {

        final Record record = Record.newBuilder(FULL_RECORD)
                                    .setAuth(Authorization.newBuilder(AUTH)
                                                          .setPassword("")
                                                          .build())
                                    .build();

        final Url url = Url.newBuilder()
                           .setRecord(record)
                           .build();

        assertEquals("http://admin@spine3.org:80/index?key=value&key2=value2#frag1",
                     UrlPrinter.printToString(url));
    }

    @Test
    public void print_url_with_broken_auth() {
        final Record record = Record.newBuilder(FULL_RECORD)
                                    .setAuth(Authorization.newBuilder(AUTH)
                                                          .setUserName("")
                                                          .build())
                                    .build();

        final Url url = Url.newBuilder()
                           .setRecord(record)
                           .build();

        // As UrlPrinter assumes that we have already validated url, it just ignores password
        // if user is not set
        assertEquals("http://spine3.org:80/index?key=value&key2=value2#frag1",
                     UrlPrinter.printToString(url));
    }

    @Test
    public void print_url_with_custom_protocol() {
        final Url.Builder url = Url.newBuilder();

        url.setRecord(Record.newBuilder()
                            .setHost(HOST)
                            .setProtocol(Record.Protocol.newBuilder()
                                                        .setName("custom")
                                                        .build())
                            .build());

        assertEquals("custom://" + HOST, UrlPrinter.printToString(url.build()));
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(UrlPrinter.class);
    }
}
