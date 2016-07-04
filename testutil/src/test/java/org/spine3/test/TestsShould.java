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

package org.spine3.test;

import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.protobuf.Durations;
import org.spine3.protobuf.Timestamps;

import static com.google.protobuf.util.TimeUtil.subtract;
import static org.junit.Assert.*;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings({"InstanceMethodNamingConvention"})
public class TestsShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Tests.class));
    }

    @Test
    public void verify_public_constructor() {
        assertFalse(Tests.hasPrivateUtilityConstructor(ClassWithPublicCtor.class));
        assertTrue(Tests.hasPrivateUtilityConstructor(TestsTest.class));
        assertTrue(Tests.hasPrivateUtilityConstructor(ClassThrowingExceptionInConstructor.class));
    }

    @Test
    public void return_current_time_in_seconds() {
        assertNotEquals(0, Tests.currentTimeSeconds());
    }

    private static class TestsTest {
        @SuppressWarnings("RedundantNoArgConstructor") // We need this constructor for our tests.
        private TestsTest() {
            // Do nothing.
        }
    }

    private static class ClassWithPublicCtor {
        public ClassWithPublicCtor() {}
    }

    private static class ClassThrowingExceptionInConstructor {
        private ClassThrowingExceptionInConstructor() {
            throw new AssertionError("Private constructor must not be called.");
        }
    }

    @Test
    public void have_frozen_time_provider() {
        final Timestamp fiveMinutesAgo = subtract(Timestamps.getCurrentTime(), Durations.ofMinutes(5));

        Timestamps.setProvider(new Tests.FrozenMadHatterParty(fiveMinutesAgo));

        assertEquals(fiveMinutesAgo, Timestamps.getCurrentTime());
    }
}
