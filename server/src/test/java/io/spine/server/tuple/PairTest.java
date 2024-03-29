/*
 * Copyright 2022, TeamDev. All rights reserved.
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
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.Time;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`Pair` should")
@SuppressWarnings({"LocalVariableNamingConvention" /* OK for tuple element values. */,
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})
class PairTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().setDefault(Message.class, TestValues.newUuidValue())
                               .setDefault(Either.class, EitherOf2.withB(Time.currentTime()))
                               .testAllPublicStaticMethods(Pair.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        var v1 = TestValues.newUuidValue();
        var v2 = TestValues.newUuidValue();

        var p1 = Pair.of(v1, v2);
        var p1a = Pair.of(v1, v2);
        var p2 = Pair.of(v2, v1);

        new EqualsTester().addEqualityGroup(p1, p1a)
                          .addEqualityGroup(p2)
                          .testEquals();
    }

    @Nested
    @DisplayName("prohibit default value for")
    class ProhibitDefault {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalArgumentException.class,
                         () -> Pair.of(StringValue.getDefaultInstance(), BoolValue.of(true)));
        }

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalArgumentException.class,
                         () -> Pair.of(BoolValue.of(false), TestValues.newUuidValue()));
        }
    }

    @Nested
    @DisplayName("prohibit Empty")
    class ProhibitEmpty {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalArgumentException.class,
                         () -> Pair.of(Empty.getDefaultInstance(), BoolValue.of(true)));
        }

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalArgumentException.class,
                         () -> Pair.of(BoolValue.of(true), Empty.getDefaultInstance()));
        }
    }

    @Test
    @DisplayName("return values")
    void returnValues() {
        var a = TestValues.newUuidValue();
        var b = BoolValue.of(true);

        var pair = Pair.of(a, b);

        assertEquals(a, pair.getA());
        assertEquals(b, pair.getB());
    }

    @Test
    @DisplayName("tell both values are present, if B isn't `Optional`")
    void tellBothValuesPresent() {
        var a = TestValues.newUuidValue();
        var b = BoolValue.of(true);

        var pair = Pair.of(a, b);

        assertThat(pair.hasA()).isTrue();
        assertThat(pair.hasB()).isTrue();
    }

    @Nested
    @DisplayName("allow `Optional` B")
    class AllowOptionalB {

        @Test
        @DisplayName("empty")
        void empty() {
            var a = TestValues.newUuidValue();
            Optional<BoolValue> b = Optional.empty();

            Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, null);

            assertEquals(a, pair.getA());
            assertEquals(b, pair.getB());

            assertThat(pair.hasB()).isFalse();
        }

        @Test
        @DisplayName("present")
        void present() {
            var a = TestValues.newUuidValue();
            var b = Optional.of(BoolValue.of(true));

            var pair = Pair.withNullable(a, b.get());

            assertEquals(a, pair.getA());
            assertEquals(b, pair.getB());

            assertThat(pair.hasB()).isTrue();
        }

        @Test
        @DisplayName("via `Optional`")
        void viaOptional() {
            var a = TestValues.newUuidValue();
            var b = Optional.of(BoolValue.of(true));

            var pair = Pair.withOptional(a, b);

            assertEquals(a, pair.getA());
            assertEquals(b, pair.getB());
        }
    }

    @Test
    @DisplayName("return Empty for empty Optional in iterator")
    void getAbsentInIterator() {
        var a = TestValues.newUuidValue();
        Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, null);

        var iterator = pair.iterator();

        assertEquals(a, iterator.next());
        assertEquals(Empty.getDefaultInstance(), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        var a = TestValues.newUuidValue();
        var b = BoolValue.of(true);

        reserializeAndAssert(Pair.of(a, b));
        reserializeAndAssert(Pair.withNullable(a, b));
        reserializeAndAssert(Pair.withNullable(a, null));

        reserializeAndAssert(Pair.withEither(a, EitherOf2.withA(Time.currentTime())));
        reserializeAndAssert(Pair.withEither(a, EitherOf2.withB(TestValues.newUuidValue())));
    }
}
