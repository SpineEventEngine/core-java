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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class, which simplifies working with {@link Url}.
 *
 * <p>Provides all necessary operations, such as conversion and validation.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class Urls {

    private Urls() {
    }

    /**
     * Converts {@link Url} with raw data into the instance with structurized record.
     *
     * @param rawUrl {@link Url} with raw String
     * @return {@link Url} with {@link org.spine3.net.Url.Record} instance
     * @throws IllegalArgumentException if the argument already has a structurized record
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static Url of(Url rawUrl) {
        checkNotNull(rawUrl);
        if (rawUrl.getValueCase() != Url.ValueCase.RAW) {
            throw new IllegalArgumentException("Given url is already built");
        }

        final String rawUrlString = rawUrl.getRaw();

        final Url url = new UrlParser(rawUrlString).parse();

        validate(url);

        return url;
    }

    /**
     * Converts String URL representation into {@link Url} instance.
     *
     * <p>Does not perform any additional validation of the value, except calling {@link Urls#validate(Url)}.
     *
     * @param rawUrlString raw URL String
     * @return {@link Url} with {@link org.spine3.net.Url.Record} instance
     */
    public static Url of(String rawUrlString) {
        checkNotNull(rawUrlString);
        final Url.Builder builder = Url.newBuilder();
        builder.setRaw(rawUrlString);
        final Url rawUrl = of(builder.build());
        return rawUrl;
    }

    /**
     * Performs String conversion for given {@link Url}.
     *
     * @param url valid {@link Url} instance
     * @return String representation of the given URL
     * @throws IllegalArgumentException if the argument is invalid
     */
    public static String toString(Url url) {
        checkNotNull(url);
        validate(url);
        final String stringUrl = UrlPrinter.printToString(url);
        return stringUrl;
    }

    /**
     * Validates {@link Url} instance.
     *
     * <ul>
     *     <li>{@link Url} with raw String is always valid.
     *     <li>{@link Url} with not set value is always invalid.
     *     <li>{@link Url} can not have empty host.
     *     <li>{@link org.spine3.net.Url.Record.Authorization} can't have password without having login.
     * </ul>
     *
     * @param url {@link Url} instance
     * @throws IllegalArgumentException in case of invalid {@link Url}
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void validate(Url url) {
        checkNotNull(url);
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
        final String user = auth.getUserName();
        final String password = auth.getPassword();

        if (user.isEmpty() && !password.isEmpty()) {
            throw new IllegalArgumentException("Url can't have password without having user name");
        }
    }
}
