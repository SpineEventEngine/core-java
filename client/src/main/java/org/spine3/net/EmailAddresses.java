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

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link EmailAddress}es.
 *
 * @author Alexander Yevsyukov
 */
public class EmailAddresses {

    private static final Pattern PATTERN = Pattern.compile(
            '(' +
            "([_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*)" +
            '@' +
            InternetDomains.pattern() +
            ')'
    );

    private EmailAddresses() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains pattern for validating email addresses.
     */
    public static Pattern pattern() {
        return PATTERN;
    }

    private static void checkArgumentIsEmailAddress(CharSequence value) {
        checkNotNull(value);
        checkArgument(pattern().matcher(value).matches());
    }

    /**
     * Creates a new {@code EmailAddress} instance for the passed value.
     *
     * @param value a valid email address
     * @return new {@code EmailAddress} instance
     * @throws IllegalArgumentException if the passed email address is not valid
     */
    public static EmailAddress valueOf(CharSequence value) {
        checkArgumentIsEmailAddress(value);

        final EmailAddress result = EmailAddress.newBuilder()
                                                .setValue(value.toString())
                                                .build();
        return result;
    }
}
