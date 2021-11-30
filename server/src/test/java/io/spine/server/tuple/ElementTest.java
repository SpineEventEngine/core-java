/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.tuple;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Message;
import com.google.protobuf.util.Durations;
import io.spine.base.Time;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.TestValues.newUuidValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Element` should")
class ElementTest {

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        var time = Time.currentTime();
        new EqualsTester().addEqualityGroup(new Element(time), new Element(time))
                          .addEqualityGroup(new Element(newUuidValue()))
                          .addEqualityGroup(new Element(Optional.empty()))
                          .testEquals();
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("elementArgument")
    @DisplayName("allow argument of type")
    void argumentType(Object arg,
                      @SuppressWarnings("unused") // is used for the type in test display names
                              String typeName) {
        assertThat(new Element(arg).value()).isEqualTo(arg);
    }

    /**
     * Provides arguments for {@link #argumentType(Object, String)}.
     *
     * <p>The first argument is the value used by the test. The second argument is the type name
     * to be used in the display test name.
     */
    @SuppressWarnings("unused") /* Used as a method source. */
    private static Stream<Object> elementArgument() {
        return Stream.of(
                Arguments.of(EitherOf2.withA(newUuidValue()), Either.class.getSimpleName()),
                Arguments.of(Optional.of(Time.currentTime()), Optional.class.getSimpleName()),
                Arguments.of(Durations.fromDays(100), Message.class.getSimpleName())
        );
    }

    @Test
    @DisplayName("do not allow types other than `Either`, `Optional`, or `Message`")
    void noOtherTypes() {
        assertThrows(IllegalArgumentException.class, () ->
                new Element(getClass().getName())
        );
    }

    @Nested
    @DisplayName("serialize if contains")
    class Serialize {

        @Test
        @DisplayName("`Message`")
        void message() {
            reserializeAndAssert(new Element(Time.currentTime()));
        }

        @Test
        @DisplayName("`Optional` with `Message`")
        void optionalMessage() {
            reserializeAndAssert(new Element(Optional.of(Time.currentTime())));
        }

        @Test
        @DisplayName("empty `Optional`")
        void emptyOptional() {
            reserializeAndAssert(new Element(Optional.empty()));
        }
    }

    @Test
    @DisplayName("restrict possible value types")
    void restrictPossibleValueTypes() {
        assertThrows(IllegalArgumentException.class, () -> new Element(getClass()));
    }
}
