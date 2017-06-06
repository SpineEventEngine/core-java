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

package io.spine.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.base.Response;
import io.spine.base.Version;
import io.spine.base.Versions;
import io.spine.protobuf.Wrapper;
import io.spine.server.entity.LifecycleFlags;
import io.spine.time.Time;
import io.spine.users.TenantId;
import io.spine.users.UserId;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.newUuid;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.lang.Math.abs;

/**
 * Utilities for testing.
 *
 * @author Alexander Yevsyukov
 */
public class Tests {

    /**
     * The prefix for generated tenant identifiers.
     */
    private static final String TENANT_PREFIX = "tenant-";

    /**
     * The prefix for generated user identifiers.
     */
    private static final String USER_PREFIX = "user-";

    private Tests() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Asserts that two booleans are equal.
     *
     * <p>This method is needed to avoid dependency on JUnit 4.x in projects that use
     * Spine and JUnit5.
     */
    @VisibleForTesting
    static void assertEquals(boolean expected, boolean actual) {
        if (expected != actual) {
            throw new AssertionError();
        }
    }

    /**
     * Asserts that a condition is true. If it isn't, it throws an
     * {@link AssertionError} without a message.
     *
     * <p>This method is needed to avoid dependency on JUnit 4.x in projects that use
     * Spine and JUnit5.
     */
    @VisibleForTesting
    static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError();
        }
    }

    /**
     * Asserts that if the passed class has private parameter-less constructor and invokes it
     * using Reflection.
     *
     * <p>Typically this method is used to add a constructor of a utility class into
     * the covered code.
     *
     * <p>Example:
     * <pre>
     * public class MyUtilityShould
     *     ...
     *     {@literal @}Test
     *     public void have_private_utility_ctor() {
     *         assertHasPrivateParameterlessCtor(MyUtility.class));
     *     }
     * </pre>
     */
    public static void assertHasPrivateParameterlessCtor(Class<?> targetClass) {
        assertTrue(hasPrivateParameterlessCtor(targetClass));
    }

    /**
     * Verifies if the passed class has private parameter-less constructor and invokes it
     * using Reflection.
     *
     * @return {@code true} if the class has private parameter-less constructor,
     *         {@code false} otherwise
     */
    @CheckReturnValue
    @VisibleForTesting
    static boolean hasPrivateParameterlessCtor(Class<?> targetClass) {
        final Constructor constructor;
        try {
            constructor = targetClass.getDeclaredConstructor();
        } catch (NoSuchMethodException ignored) {
            return false;
        }

        if (!Modifier.isPrivate(constructor.getModifiers())) {
            return false;
        }

        constructor.setAccessible(true);

        //noinspection OverlyBroadCatchBlock
        try {
            // Call the constructor to include it into the coverage.

            // Some of the coding conventions may encourage throwing AssertionError
            // to prevent the instantiation of the target class,
            // if it is designed as a utility class.
            constructor.newInstance();
        } catch (Exception ignored) {
            return true;
        }
        return true;
    }

    /**
     * Returns {@code null}.
     * Use it when it is needed to pass {@code null} to a method in tests so that no
     * warnings suppression is needed.
     */
    public static <T> T nullRef() {
        final T nullRef = null;
        return nullRef;
    }

    /**
     * Creates a new user ID instance by passed string value.
     *
     * @param value new user ID value
     * @return new instance
     */
    public static UserId newUserId(String value) {
        checkNotNull(value);

        return UserId.newBuilder()
                .setValue(value)
                .build();
    }

    /**
     * Generates a new UUID-based {@code UserId}.
     */
    public static UserId newUserUuid() {
        return newUserId(USER_PREFIX + newUuid());
    }

    /**
     * Generates a new UUID-based {@code TenantId}.
     */
    public static TenantId newTenantUuid() {
        return newTenantId(TENANT_PREFIX + newUuid());
    }

    /**
     * Creates a new {@code TenantId} with the passed value.
     *
     * @param value must be non-null, not empty, and not-blank
     * @return new {@code TenantId}
     */
    public static TenantId newTenantId(String value) {
        checkNotEmptyOrBlank(value, TenantId.class.getSimpleName());
        return TenantId.newBuilder()
                       .setValue(value)
                       .build();
    }

    /**
     * Creates a test instance of {@code TenantId} with the simple name of the passed test class.
     */
    public static TenantId newTenantId(Class<?> testClass) {
        return newTenantId(testClass.getSimpleName());
    }
    /**
     * Generates a {@code StringValue} with generated UUID.
     *
     * <p>Use this method when you need to generate a test {@code Message} value
     * but do not want to resort to {@code Timestamp} via {@code Timestamps#getCurrentTime()}.
     */
    public static StringValue newUuidValue() {
        return Wrapper.forString(newUuid());
    }

    /**
     * Asserts that the passed message has a field that matches the passed field mask.
     *
     * @throws AssertionError if the check fails
     */
    public static void assertMatchesMask(Message message, FieldMask fieldMask) {
        final List<String> paths = fieldMask.getPathsList();

        for (Descriptors.FieldDescriptor field : message.getDescriptorForType()
                                                        .getFields()) {
            if (field.isRepeated()) {
                continue;
            }
            assertEquals(message.hasField(field), paths.contains(field.getFullName()));
        }
    }

    /**
     * Factory method for creating versions from tests.
     */
    public static Version newVersionWithNumber(int number) {
        return Versions.newVersion(number, Time.getCurrentTime());
    }

    /**
     * Returns {@code StreamObserver} that records the responses.
     *
     * <p>Use this method when you need to verify the responses of calls like
     * {@link io.spine.server.commandbus.CommandBus#post(io.spine.base.Command, StreamObserver)
     * CommandBus.post()} and similar methods.
     *
     * <p>Returns a fresh instance upon every call to avoid state clashes.
     */
    public static MemoizingObserver memoizingObserver() {
        return new MemoizingObserver();
    }

    public static void assertSecondsEqual(long expectedSec, long actualSec, long maxDiffSec) {
        final long diffSec = abs(expectedSec - actualSec);
        assertTrue(diffSec <= maxDiffSec);
    }

    /**
     * Generates a random integer in the range [0, max).
     */
    public static int random(int max) {
        return random(0, max);
    }

    /**
     * Generates a random integer in the range [min, max).
     */
    public static int random(int min, int max) {
        int randomNum = ThreadLocalRandom.current().nextInt(min, max);
        return randomNum;
    }

    /**
     * The {@code StreamObserver} recording the responses.
     *
     * @see #memoizingObserver()
     */
    public static class MemoizingObserver implements StreamObserver<Response> {

        private Response response;
        private Throwable throwable;
        private boolean completed = false;

        protected MemoizingObserver() {
        }

        @Override
        public void onNext(Response response) {
            this.response = response;
        }

        @Override
        public void onError(Throwable throwable) {
            this.throwable = throwable;
        }

        @Override
        public void onCompleted() {
            this.completed = true;
        }

        public Response getResponse() {
            return response;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isCompleted() {
            return this.completed;
        }
    }

    /**
     * Creates {@code Visibility} with archived flag set to {@code true}.
     */
    public static LifecycleFlags archived() {
        return LifecycleFlags.newBuilder()
                             .setArchived(true)
                             .build();
    }
    /**
     * Creates {@code Visibility} with deleted flag set to {@code true}.
     */
    public static LifecycleFlags deleted() {
        return LifecycleFlags.newBuilder()
                             .setDeleted(true)
                             .build();
    }
}
