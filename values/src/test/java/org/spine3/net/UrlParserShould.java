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

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection"})
public class UrlParserShould {

    private static final String HOST = "ulr-parser-should.com";
    private static final String HTTP_PROTOCOL = "http";
    private static final String UNKNOWN_PROTOCOL = "http5";
    private static final String PROTOCOL_HOST = HTTP_PROTOCOL + "://" + HOST;
    private static final String UNKNOWN_PROTOCOL_HOST = UNKNOWN_PROTOCOL + "://" + HOST;
    private static final String PORT = "8080";

    @Test
    public void parse_protocol_and_host() {
        final Url protocolHost = new UrlParser(PROTOCOL_HOST).parse();

        final Url.Record record = protocolHost.getRecord();
        assertEquals(HOST, record.getHost());
        assertEquals(Url.Record.Schema.HTTP, record.getProtocol()
                                                   .getSchema());
    }

    @Test
    public void parse_host() {
        final Url protocolHost = new UrlParser(HOST).parse();

        final Url.Record record = protocolHost.getRecord();

        assertEquals(HOST, record.getHost());
        assertEquals(Url.Record.Schema.UNDEFINED, record.getProtocol()
                                                        .getSchema());
    }

    @Test
    public void parse_unknown_protocol() {
        final Url protocolHost = new UrlParser(UNKNOWN_PROTOCOL_HOST).parse();

        final Url.Record record = protocolHost.getRecord();
        assertEquals(UNKNOWN_PROTOCOL, record.getProtocol()
                                             .getName());
    }

    @Test
    public void parse_credentials() {
        final String userName = "admin";
        final String password = "root";

        final String userUrl = HTTP_PROTOCOL + "://" + userName + '@' + HOST;
        final String userPasswordUrl = HTTP_PROTOCOL + "://" + userName + ':' + password + '@' + HOST;

        final Url.Record record1 = new UrlParser(userUrl).parse()
                                                         .getRecord();
        final String user1 = record1
                .getAuth()
                .getUserName();
        assertEquals(userName, user1);

        final Url.Record record2 = new UrlParser(userPasswordUrl).parse()
                                                                 .getRecord();
        final Url.Record.Authorization auth2 = record2.getAuth();
        final String user2 = auth2.getUserName();
        assertEquals(userName, user2);
        assertEquals(password, auth2.getPassword());
    }

    @Test
    public void parse_port() {
        final String url = HOST + ':' + PORT;

        final Url parsedUrl = new UrlParser(url).parse();

        assertEquals(PORT, parsedUrl.getRecord()
                                    .getPort());
    }

    @Test
    public void parse_path() {
        final String resource = "index/2";
        final String rawUrl = HOST + '/' + resource;

        final Url url = new UrlParser(rawUrl).parse();

        assertEquals(resource, url.getRecord()
                                  .getPath());
    }

    @Test
    public void parse_fragment() {
        final String fragment = "reference";
        final String rawUrl = HOST + "/index/2#" + fragment;

        final Url url = new UrlParser(rawUrl).parse();

        assertEquals(fragment, url.getRecord()
                                  .getFragment());
    }

    @Test
    public void parse_queries() {
        final String key1 = "key1";
        final String key2 = "key2";

        final String value1 = "value1";
        final String value2 = "value2";

        final String query1 = key1 + '=' + value1;
        final String query2 = key2 + '=' + value2;

        final String rawUrl = HOST + '?' + query1 + '&' + query2;

        final Url url = new UrlParser(rawUrl).parse();

        final List<Url.Record.QueryParameter> queries = url.getRecord()
                                                             .getQueryList();

        assertEquals(2, queries.size());

        final Url.Record.QueryParameter queryInstance1 = queries.get(0);
        final Url.Record.QueryParameter queryInstance2 = queries.get(1);

        assertEquals(key1, queryInstance1.getKey());
        assertEquals(value1, queryInstance1.getValue());
        assertEquals(key2, queryInstance2.getKey());
        assertEquals(value2, queryInstance2.getValue());
    }

    @Test
    public void parse_url_with_all_sub_items() {
        final String rawUrl = "https://user:password@spine3.org/index?auth=none&locale=us#fragment9";

        final Url.Record record = new UrlParser(rawUrl).parse().getRecord();

        assertEquals(Url.Record.Schema.HTTPS, record.getProtocol().getSchema());
        assertEquals("user", record.getAuth().getUserName());
        assertEquals("password", record.getAuth().getPassword());
        assertEquals("spine3.org", record.getHost());
        assertEquals("index", record.getPath());
        assertEquals("auth=none", QueryParameters.toString(record.getQuery(0)));
        assertEquals("locale=us", QueryParameters.toString(record.getQuery(1)));
        assertEquals("fragment9", record.getFragment());
    }
}
