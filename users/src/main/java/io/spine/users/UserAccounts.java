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

package io.spine.users;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

/**
 * Utility routines for working with {@link UserAccount}.
 */
public class UserAccounts {

    @VisibleForTesting
    static final String GOOGLE_AUTH_PROVIDER_ID = "google.com";

    private static final int DUNBARS_NUMBER = 150;

    private UserAccounts() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains a suggested congnitive limit to the number of people with whom one can
     * maintain stable social relationships.
     *
     * <p>This value should be used for calculating the cache size for storing
     * {@code UserAccount} instances.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Dunbar%27s_number">Dunbar's number</a>
     */
    public static int getDunbarsNumber() {
        return DUNBARS_NUMBER;
    }

    /**
     * Obtains Google account ID from the passed {@code UserAccount} instance.
     *
     * @return account ID or empty optional if {@code google.com} is not listed
     * as an identity provider of the passed account
     */
    public static Optional<String> getGoogleUid(UserAccount account) {
        for (UserInfo userInfo : account.getLinkedIdentityList()) {
            if (userInfo.getProviderId().equals(GOOGLE_AUTH_PROVIDER_ID)) {
                return Optional.of(userInfo.getUid());
            }
        }
        return Optional.absent();
    }
}
