/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import org.spine3.net.Url.Record.Protocol;

/* package */ class UrlParser {

    /* package */ static final String PROTOCOL_ENDING = "://";
    /* package */ static final String CREDENTIALS_ENDING = "@";
    /* package */ static final String CREDENTIALS_SEPARATOR = ":";
    /* package */ static final String HOST_ENDING = "/";
    /* package */ static final String HOST_PORT_SEPARATOR = ":";

    /* package */ static final String FRAGMENT_START = "#";
    /* package */ static final String QUERIES_START = "?";
    /* package */ static final String QUERIES_SEPARATOR = "&";

    private final String originalUrl;

    private Url.Record.Builder record;
    private String unProcessedInput;

    /* package */ UrlParser(String url) {
        this.originalUrl = url;
    }

    /* package */ Url parse() {
        setupStartingState();

        parseProtocol();
        parseCredentials();
        parseHost();
        parseFragment();
        parseQueries();
        parsePath();

        final Url.Builder result = Url.newBuilder();
        result.setRecord(record);

        return result.build();
    }

    private void setupStartingState() {
        record = Url.Record.newBuilder();
        unProcessedInput = originalUrl;
    }

    private void parseProtocol() {
        final Protocol.Builder protocolBuilder = Protocol.newBuilder();
        final int protocolEndingIndex = unProcessedInput.indexOf(PROTOCOL_ENDING);
        if (protocolEndingIndex == -1) {
            protocolBuilder.setSchema(Url.Record.Schema.UNDEFINED);
            record.setProtocol(protocolBuilder);
            return;
        }
        final String protocol = unProcessedInput.substring(0, protocolEndingIndex);
        unProcessedInput = unProcessedInput.substring(protocolEndingIndex + PROTOCOL_ENDING.length());

        final Url.Record.Schema schema = Schemas.of(protocol);

        if (schema == Url.Record.Schema.UNDEFINED) {
            protocolBuilder.setName(protocol);
        } else {
            protocolBuilder.setSchema(schema);
        }

        record.setProtocol(protocolBuilder.build());
    }

    private void parseCredentials() {
        final int credentialsEndingIndex = unProcessedInput.indexOf(CREDENTIALS_ENDING);
        if (credentialsEndingIndex == -1) {
            return;
        }

        final String credential = unProcessedInput.substring(0, credentialsEndingIndex);
        unProcessedInput = unProcessedInput.substring(credentialsEndingIndex + CREDENTIALS_ENDING.length());

        final Url.Record.Authorization.Builder auth = Url.Record.Authorization.newBuilder();

        final int credentialsSeparatorIndex = credential.indexOf(CREDENTIALS_SEPARATOR);
        if (credentialsSeparatorIndex != -1) {
            final String user = credential.substring(0, credentialsSeparatorIndex);
            final String password = credential.substring(
                    credentialsSeparatorIndex + CREDENTIALS_SEPARATOR.length());
            auth.setPassword(password);
            auth.setUser(user);
        } else {
            auth.setUser(credential);
        }

        record.setAuth(auth.build());
    }

    private void parseHost() {
        final int hostEndingIndex = unProcessedInput.indexOf(HOST_ENDING);
        final String host;

        if (hostEndingIndex == -1) {
            host = unProcessedInput;
            unProcessedInput = "";
        } else {
            host = unProcessedInput.substring(0, hostEndingIndex);
            unProcessedInput = unProcessedInput.substring(hostEndingIndex + HOST_ENDING.length());
        }

        final int portIndex = host.indexOf(HOST_PORT_SEPARATOR);
        if (portIndex != -1) {
            final String port = host.substring(portIndex + HOST_PORT_SEPARATOR.length());
            record.setPort(port);
            final String hostAddress = host.substring(0, portIndex);
            record.setHost(hostAddress);
        } else {
            record.setHost(host);
        }
    }

    private void parseFragment() {
        if (unProcessedInput.isEmpty()) {
            return;
        }

        final int fragmentIndex = unProcessedInput.lastIndexOf(FRAGMENT_START);
        if (fragmentIndex == -1) {
            return;
        }

        final String fragment = unProcessedInput.substring(fragmentIndex + FRAGMENT_START.length());
        unProcessedInput = unProcessedInput.substring(0, fragmentIndex);

        record.setFragment(fragment);
    }

    private void parseQueries() {
        if (unProcessedInput.isEmpty()) {
            return;
        }

        final int queriesStartIndex = unProcessedInput.indexOf(QUERIES_START);
        if (queriesStartIndex == -1) {
            return;
        }

        final String queriesString = unProcessedInput.substring(queriesStartIndex + QUERIES_START.length());
        unProcessedInput = unProcessedInput.substring(0, queriesStartIndex);

        final String[] queries = queriesString.split(QUERIES_SEPARATOR);
        for (String query : queries) {
            // TODO:2016-05-23:mikhail.mikhaylov: Split arguments if needed.
            record.addQuery(query);
        }
    }

    private void parsePath() {
        if (unProcessedInput.isEmpty()) {
            return;
        }

        record.setPath(unProcessedInput);
        unProcessedInput = "";
    }
}
