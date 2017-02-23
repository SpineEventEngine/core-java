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

package org.spine3.base;

import com.google.common.testing.NullPointerTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.idToAny;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Stringifiers.idToString;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

/**
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "ClassWithTooManyMethods"})
public class IdentifiersShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Identifiers.class));
    }


    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_object_of_unsupported_class_passed() {
        //noinspection UnnecessaryBoxing
        idToString(Boolean.valueOf(true));
    }

    @SuppressWarnings("UnnecessaryBoxing") // OK as we want to show types clearly.
    @Test
    public void convert_to_string_number_ids() {
        assertEquals("10", idToString(Integer.valueOf(10)));
        assertEquals("100", idToString(Long.valueOf(100)));
    }

    @Test
    public void generate_new_UUID() {
        // We have non-empty values.
        assertTrue(newUuid().length() > 0);

        // Values are random.
        assertNotEquals(newUuid(), newUuid());
    }

    @Test(expected = IllegalArgumentException.class)
    public void do_not_convert_unsupported_ID_type_to_Any() {
        //noinspection UnnecessaryBoxing
        idToAny(Boolean.valueOf(false));
    }

    @Test
    public void pass_the_null_tolerance_check() {
        new NullPointerTester()
                .testStaticMethods(Identifiers.class, NullPointerTester.Visibility.PACKAGE);
    }
}
