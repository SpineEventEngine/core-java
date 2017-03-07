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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.net.InternetDomains.pattern;
import static org.spine3.net.InternetDomains.valueOf;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
public class InternetDomainsShould {

    @Test
    public void have_utility_ctor() {
        assertHasPrivateParameterlessCtor(InternetDomains.class);
    }

    @Test
    public void provide_matching_pattern() {
        assertTrue(pattern().matcher("spine3.org")
                            .matches());

        assertTrue(pattern().matcher("teamdev.com").matches());
        assertTrue(pattern().matcher("a.com").matches());
        assertTrue(pattern().matcher("boeng747.aero").matches());

        assertFalse(pattern().matcher(".com").matches());
        assertFalse(pattern().matcher("com").matches());
        assertFalse(pattern().matcher("192.168.0.1").matches());
    }

    @Test
    public void create_InternetDomain_instance() {
        final String domainName = "example.org";

        assertEquals(domainName, valueOf(domainName).getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reject_invalid_name() {
        valueOf("1.0");
    }
}
