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
    public Set<FieldValidatingOption<?>> forBoolean() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forByteString() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forDouble() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forEnum() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forFloat() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forInt() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forLong() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forMessage() {
        return fakeOption();
    }

    @Override
    public Set<FieldValidatingOption<?>> forString() {
        return fakeOption();
    }

    private static Set<FieldValidatingOption<?>> fakeOption() {
        FieldValidatingOption<?> option = new FakeOption();
        return ImmutableSet.of(option);
    }
}
