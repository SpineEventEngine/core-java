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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.net.EmailAddresses.valueOf;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
public class EmailAddressesShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(EmailAddresses.class);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EmailAddresses.class);
    }

    @Test
    public void provide_pattern() {
        assertTrue(patternMatches("user@site.com"));
        assertTrue(patternMatches("a@b.com"));
        assertTrue(patternMatches("a@b-c.com"));

        assertFalse(patternMatches("@site.org"));
        assertFalse(patternMatches("user@"));
        assertFalse(patternMatches("user @ site.com"));
    }

    private static boolean patternMatches(CharSequence input) {
        return EmailAddresses.pattern().matcher(input).matches();
    }

    @Test
    public void create_EmailAddress_instance() {
        final String email = "jdoe@spine3.org";

        assertEquals(email, valueOf(email).getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_invalid_email() {
        valueOf("fiz baz");
    }
}
