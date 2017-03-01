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

import com.google.common.testing.NullPointerTester;
import org.junit.Test;
import org.spine3.net.Url.Record;
import org.spine3.net.Url.Record.Authorization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "LocalVariableNamingConvention"})
public class UrlsShould {

    @Test
    public void convert_proper_urls() {
        final Url url = Urls.of("http://convert-proper-url.com");

        assertEquals("convert-proper-url.com", url.getRecord()
                                                  .getHost());
        assertEquals(Record.Schema.HTTP, url.getRecord()
                                            .getProtocol()
                                            .getSchema());
    }

    @Test
    public void fail_on_already_formed_url() {
        final Url url = Urls.of("http://already-formed-url.com");

        try {
            Urls.of(url);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void validate_raw_urls() {
        final Url url1 = Url.newBuilder()
                            .setRaw("validate-raw.com")
                            .build();
        Urls.validate(url1);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void fail_on_wrong_urls_validation() {
        final Url emptyUrl = Url.newBuilder()
                                .build();
        try {
            Urls.toString(emptyUrl);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        final Url urlWithoutHost = Url.newBuilder()
                                      .setRecord(Record.getDefaultInstance())
                                      .build();
        try {
            Urls.toString(urlWithoutHost);
            fail();
        } catch (IllegalArgumentException ignored) {
        }

        final Authorization authWithPasswordWithoutUser =
                Authorization.newBuilder()
                             .setPassword("password")
                             .build();
        final Record recordWithPasswordWithoutUser =
                Record.newBuilder()
                      .setHost("some-url.com")
                      .setAuth(authWithPasswordWithoutUser)
                      .build();

        final Url urlWithPasswordWithoutUser =
                Url.newBuilder()
                   .setRecord(recordWithPasswordWithoutUser)
                   .build();
        try {
            Urls.toString(urlWithPasswordWithoutUser);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void convert_to_string_properly() {
        final String rawUrl = "http://foo-bar.com/index";

        assertEquals(rawUrl, Urls.toString(Urls.of(rawUrl)));
    }

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Urls.class);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Urls.class);
    }
}
