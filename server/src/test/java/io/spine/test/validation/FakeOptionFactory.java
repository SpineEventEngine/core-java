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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import io.spine.validate.FieldValidatingOption;
import io.spine.validate.ValidatingOptionFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.auto.service.AutoService;

import java.util.Set;

@AutoService(ValidatingOptionFactory.class)
@SuppressWarnings("Immutable")
public final class FakeOptionFactory implements ValidatingOptionFactory {

    private static @Nullable RuntimeException exception = null;

    public static void shouldFailWith(RuntimeException e) {
        exception = e;
    }

    public static void shouldNowFail() {
        exception = null;
    }

    public static @Nullable RuntimeException plannedException() {
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
