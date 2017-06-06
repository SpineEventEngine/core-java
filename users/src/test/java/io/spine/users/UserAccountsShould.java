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

import org.junit.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.users.UserAccounts.GOOGLE_AUTH_PROVIDER_ID;
import static io.spine.users.UserAccounts.getGoogleUid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserAccountsShould {

    @Test
    public void have_private_utility_ctor() {
        assertHasPrivateParameterlessCtor(UserAccounts.class);
    }

    @Test
    public void return_absent_for_google_uid_if_no_linked_account_added() {
        assertFalse(UserAccounts.getGoogleUid(UserAccount.getDefaultInstance()).isPresent());
    }

    @SuppressWarnings({"ConstantConditions", "MagicNumber"})
    // Checking the bounds of the constant {@code DUNBARS_NUMBER} value.
    @Test
    public void declare_Dubars_number() {
        assertTrue(UserAccounts.getDunbarsNumber() > 100
                   && UserAccounts.getDunbarsNumber() < 250);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // No need to check in this test.
    @Test
    public void obtain_user_google_uid() {
        final String uid = newUuid(); // In reality Google's user ID has different formant.

        final UserAccount userAccount =
                UserAccount.newBuilder()
                           .addLinkedIdentity(UserInfo.newBuilder()
                                                      .setProviderId(GOOGLE_AUTH_PROVIDER_ID)
                                                      .setUid(uid))
                           .build();

        assertEquals(uid, getGoogleUid(userAccount).get());
    }
}
