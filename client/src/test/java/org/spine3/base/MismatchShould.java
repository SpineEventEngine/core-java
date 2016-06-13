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

package org.spine3.base;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;
/**
 * @author Andrey Lavrov
 */
public class MismatchShould {

    private static final String REQUESTED = "requested";
    private static final String EXPECTED = "expected";
    private static final String FOUND = "found";
    private static final int VERSION = 0;

    @Test
    public void has_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Mismatch.class));
    }

    @Test
    public void return_value_with_default_expected_field_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(null, FOUND, REQUESTED, VERSION);
        final String expected = mismatch.getExpected()
                                        .toString();
        assertEquals("", expected);
    }

    @Test
    public void return_value_with_default_found_field_if_it_was_passed_as_null() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, null, REQUESTED, VERSION);
        final String actual = mismatch.getActual()
                                      .toString();
        assertEquals("", actual);
    }

    @Test
    public void return_value_with_passed_values() {
        final ValueMismatch mismatch = Mismatch.of(EXPECTED, FOUND, REQUESTED, VERSION);
        assertTrue(mismatch.hasExpected());
        assertTrue(mismatch.hasActual());
        assertTrue(mismatch.hasRequested());
    }
}
