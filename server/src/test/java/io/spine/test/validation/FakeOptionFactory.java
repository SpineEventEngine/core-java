/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.test.validation;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.spine.validate.option.FieldValidatingOption;
import io.spine.validate.option.ValidatingOptionFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

/**
 * A test-only implementation of {@link ValidatingOptionFactory}.
 *
 * <p>If a message validation should fail for the testing purposes, specify the expected exception
 * via {@link #shouldFailWith(RuntimeException)}. When the test is over, call
 * {@link #shouldNotFail()} in order to fix validation.
 */
@AutoService(ValidatingOptionFactory.class)
@SuppressWarnings("Immutable")
public final class FakeOptionFactory implements ValidatingOptionFactory {

    private static @Nullable RuntimeException exception = null;

    /**
     * Sets the exception to throw at any message validation.
     */
    public static void shouldFailWith(RuntimeException e) {
        exception = e;
    }

    /**
     * Resets the message validation.
     *
     * @see #shouldFailWith(RuntimeException)
     */
    public static void shouldNotFail() {
        exception = null;
    }

    /**
     * Obtains the exception to throw on validation.
     */
    static @Nullable RuntimeException plannedException() {
        return exception;
    }

    @Override
    public Set<FieldValidatingOption<?, Boolean>> forBoolean() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, ByteString>> forByteString() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, Double>> forDouble() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, Descriptors.EnumValueDescriptor>> forEnum() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, Float>> forFloat() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, Integer>> forInt() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, Long>> forLong() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, Message>> forMessage() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?, String>> forString() {
        return fakeOption();
    }

    private static <T> Set<FieldValidatingOption<?, T>> fakeOption() {
        FakeOption option = new FakeOption();
        @SuppressWarnings("unchecked") // OK for tests.
        FieldValidatingOption<?, T> castOption = (FieldValidatingOption<?, T>) option;
        return ImmutableSet.of(castOption);
    }
}
