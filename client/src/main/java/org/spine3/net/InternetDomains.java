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
import static org.spine3.net.Patterns.HOST_NAME_PATTERN;

/**
 * Utility class for working with {@link InternetDomain}s.
 *
 * @author Alexander Yevsyukov
 */
public class InternetDomains {

    private InternetDomains() {
        // Prevent instantiation of this  utility class.
    }

    /**
     * Obtains {@code Pattern} for checking an Internet host name.
     *
     * <p>The pattern does not accept IP addresses.
     */
    public static Pattern pattern() {
        return HOST_NAME_PATTERN;
    }

    private static void checkArgumentIsDomainName(CharSequence name) {
        checkArgument(pattern().matcher(name)
                               .matches());
    }

    /**
     * Creates a new {@code InternetDomain} instance for the passed name.
     *
     * @param name a valid Internet domain name
     * @return new {@code InternetDomain} instance
     * @throws IllegalArgumentException if the passed domain name is not valid
     */
    public static InternetDomain valueOf(CharSequence name) {
        checkNotNull(name);
        checkArgumentIsDomainName(name);

        final InternetDomain result = InternetDomain.newBuilder()
                                                    .setValue(name.toString())
                                                    .build();
        return result;
    }
}
