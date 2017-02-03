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

import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import org.spine3.base.Command;
import org.spine3.base.Identifiers;
import org.spine3.base.Response;
import org.spine3.protobuf.Timestamps;
import org.spine3.protobuf.Values;
import org.spine3.server.command.CommandBus;
import org.spine3.users.UserId;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Utilities for testing.
 *
 * @author Alexander Yevsyukov
 */
public class Tests {

    private Tests() {}

    /**
     * Verifies if the passed class has private parameter-less constructor and invokes it
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
     *         assertTrue(hasPrivateParameterlessCtor(MyUtility.class));
     *     }
     * </pre>
     * @return true if the class has private parameter-less constructor
     */
    public static boolean hasPrivateParameterlessCtor(Class<?> targetClass) {
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
     * Returns the current time in seconds via {@link Timestamps#getCurrentTime()}.
     *
     * @return a seconds value
     */
    public static long currentTimeSeconds() {
        final long secs = Timestamps.getCurrentTime().getSeconds();
        return secs;
    }

    /**
     * Returns {@code null}.
     * Use it when it is needed to pass {@code null} to a method in tests so that no warnings suppression is needed.
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
        return newUserId(Identifiers.newUuid());
    }

    /**
     * Generates a {@code StringValue} with generated UUID.
     *
     * <p>Use this method when you need to generate a test {@code Message} value
     * but do not want to resort to {@code Timestamp} via {@code Timestamps#getCurrentTime()}.
     */
    public static StringValue newUuidValue() {
        return Values.newStringValue(Identifiers.newUuid());
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
     * Adjusts a timestamp in the context of the passed command.
     *
     * @return new command instance with the modified timestamp
     */
    public static Command adjustTimestamp(Command command, Timestamp timestamp) {
        final Command.Builder commandBuilder =
                command.toBuilder()
                       .setContext(command.getContext()
                                          .toBuilder()
                                          .setTimestamp(timestamp));
        return commandBuilder.build();
    }

    /**
     * The provider of current time, which is always the same.
     *
     * <p>Use this {@code Timestamps.Provider} in time-related tests that are sensitive to
     * bounds of minutes, hours, days, etc.
     */
    public static class FrozenMadHatterParty implements Timestamps.Provider {
        private final Timestamp frozenTime;

        public FrozenMadHatterParty(Timestamp frozenTime) {
            this.frozenTime = frozenTime;
        }

        /** Returns the value passed to the constructor. */
        @Override
        public Timestamp getCurrentTime() {
            return frozenTime;
        }
    }

    /**
     * The {@code StreamObserver} which does nothing.
     * @see #emptyObserver()
     */
    private static final StreamObserver<Response> emptyObserver = new StreamObserver<Response>() {
        @Override
        public void onNext(Response value) {
            // Do nothing.
        }

        @Override
        public void onError(Throwable t) {
            // Do nothing.
        }

        @Override
        public void onCompleted() {
            // Do nothing.
        }
    };

    /**
     * Returns {@code StringObserver} that does nothing.
     *
     * <p>Use this method when you need to call {@link CommandBus#post(Command, StreamObserver)}
     * and observing results is not needed.
     */
    public static StreamObserver<Response> emptyObserver() {
        return emptyObserver;
    }
}
