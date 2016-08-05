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

import static com.google.protobuf.util.Timestamps.subtract;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateUtilityConstructor;

@SuppressWarnings({"InstanceMethodNamingConvention"})
public class TestsShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateUtilityConstructor(Tests.class));
    }

    @Test
    public void return_false_if_no_private_but_public_ctor() {
        assertFalse(Tests.hasPrivateUtilityConstructor(ClassWithPublicCtor.class));
    }

    @Test
    public void return_false_if_only_ctor_with_args_found() {
        assertFalse(Tests.hasPrivateUtilityConstructor(ClassWithCtorWithArgs.class));
    }

    @Test
    public void return_true_if_class_has_private_ctor() {
        assertTrue(Tests.hasPrivateUtilityConstructor(ClassWithPrivateCtor.class));
    }

    @Test
    public void return_true_if_class_has_private_throwing_ctor() {
        assertTrue(Tests.hasPrivateUtilityConstructor(ClassThrowingExceptionInConstructor.class));
    }

    @Test
    public void return_current_time_in_seconds() {
        assertNotEquals(0, Tests.currentTimeSeconds());
    }

    @Test
    public void return_null_reference() {
        assertNull(Tests.nullRef());
    }

    @Test
    public void have_frozen_time_provider() {
        final Timestamp fiveMinutesAgo = subtract(Timestamps.getCurrentTime(), Durations.ofMinutes(5));

        final Tests.FrozenMadHatterParty provider = new Tests.FrozenMadHatterParty(fiveMinutesAgo);

        assertEquals(fiveMinutesAgo, provider.getCurrentTime());
    }

    private static class ClassWithPrivateCtor {
        @SuppressWarnings("RedundantNoArgConstructor") // We need this constructor for our tests.
        private ClassWithPrivateCtor() {}
    }

    private static class ClassWithPublicCtor {
        public ClassWithPublicCtor() {}
    }

    private static class ClassThrowingExceptionInConstructor {
        private ClassThrowingExceptionInConstructor() {
            throw new AssertionError("Private constructor must not be called.");
        }
    }

    private static class ClassWithCtorWithArgs {
        private ClassWithCtorWithArgs(int i) {}
    }
}
