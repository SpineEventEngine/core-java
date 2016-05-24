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

@SuppressWarnings("UtilityClass")
public class Urls {

    private Urls() {
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static Url of(Url rawUrl) {
        if (rawUrl.getValueCase() != Url.ValueCase.RAW) {
            throw new IllegalArgumentException("Given url is already built");
        }

        final String rawUrlString = rawUrl.getRaw();

        final Url url = new UrlParser(rawUrlString).parse();

        validate(url);

        return url;
    }

    public static Url of(String rawUrlString) {
        final Url.Builder builder = Url.newBuilder();
        builder.setRaw(rawUrlString);
        final Url rawUrl = of(builder.build());
        return rawUrl;
    }

    public static String toString(Url url) {
        validate(url);
        final String stringUrl = UrlPrinter.printToString(url);
        return stringUrl;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    public static void validate(Url url) {
        if (url.getValueCase() == Url.ValueCase.VALUE_NOT_SET) {
            throw new IllegalArgumentException("Url is empty");
        }

        if (url.getValueCase() == Url.ValueCase.RAW) {
            return;
        }

        final Url.Record record = url.getRecord();
        final String host = record.getHost();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Url host can not be empty!");
        }

        final Url.Record.Authorization auth = record.getAuth();
        final String user = auth.getUser();
        final String password = auth.getPassword();

        if (user.isEmpty() && !password.isEmpty()) {
            throw new IllegalArgumentException("Url can't have password without having user name");
        }
    }
}
