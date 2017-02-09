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

package org.spine3.test;

import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.spine3.base.Response;
import org.spine3.protobuf.Durations;
import org.spine3.protobuf.Timestamps;
import org.spine3.users.UserId;

import static com.google.protobuf.util.Timestamps.subtract;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;
import static org.spine3.test.Tests.newUserId;

public class TestsShould {

    @Test
    public void have_private_constructor() {
        assertTrue(hasPrivateParameterlessCtor(Tests.class));
    }

    @Test
    public void return_false_if_no_private_but_public_ctor() {
        assertFalse(Tests.hasPrivateParameterlessCtor(ClassWithPublicCtor.class));
    }

    @Test
    public void return_false_if_only_ctor_with_args_found() {
        assertFalse(Tests.hasPrivateParameterlessCtor(ClassWithCtorWithArgs.class));
    }

    @Test
    public void return_true_if_class_has_private_ctor() {
        assertTrue(Tests.hasPrivateParameterlessCtor(ClassWithPrivateCtor.class));
    }

    @Test
    public void return_true_if_class_has_private_throwing_ctor() {
        assertTrue(Tests.hasPrivateParameterlessCtor(ClassThrowingExceptionInConstructor.class));
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
    public void create_UserId_by_string() {

        final String testIdString = "12345";
        final UserId userId = newUserId(testIdString);

        final UserId expected = UserId.newBuilder().setValue(testIdString).build();

        assertEquals(expected, userId);
    }

    @Test
    public void create_new_UUID_based_UserId() {
        assertFalse(Tests.newUserUuid().getValue().isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void do_not_accept_null_UseId_value() {
        newUserId(Tests.<String>nullRef());
    }

    @Test
    public void have_frozen_time_provider() {
        final Timestamp fiveMinutesAgo = subtract(Timestamps.getCurrentTime(), Durations.ofMinutes(5));

        final Tests.FrozenMadHatterParty provider = new Tests.FrozenMadHatterParty(fiveMinutesAgo);

        assertEquals(fiveMinutesAgo, provider.getCurrentTime());
    }

    @Test
    public void return_empty_StreamObserver() {
        final StreamObserver<Response> emptyObserver = Tests.emptyObserver();
        assertNotNull(emptyObserver);
        // Call methods just to add to coverage.
        emptyObserver.onNext(Tests.<Response>nullRef());
        emptyObserver.onError(Tests.<Throwable>nullRef());
        emptyObserver.onCompleted();
    }

    private static class ClassWithPrivateCtor {
        @SuppressWarnings("RedundantNoArgConstructor") // We need this constructor for our tests.
        private ClassWithPrivateCtor() {}
    }

    private static class ClassWithPublicCtor {
        @SuppressWarnings("PublicConstructorInNonPublicClass") // It's the purpose of this tests class.
        public ClassWithPublicCtor() {}
    }

    private static class ClassThrowingExceptionInConstructor {
        private ClassThrowingExceptionInConstructor() {
            throw new AssertionError("This private constructor must not be called.");
        }
    }

    private static class ClassWithCtorWithArgs {
        @SuppressWarnings("unused")
        private final int id;
        private ClassWithCtorWithArgs(int id) { this.id = id;}
    }
}
