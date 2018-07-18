/*
 * Copyright 2018, TeamDev. All rights reserved.
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
import io.spine.test.TestValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"LocalVariableNamingConvention" /* OK for tuple element values. */,
        "InnerClassMayBeStatic", "ClassCanBeStatic" /* JUnit nested classes cannot be static. */,
        "DuplicateStringLiteralInspection" /* Common test display names. */})

@DisplayName("Pair should")
class PairTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().setDefault(Message.class, TestValues.newUuidValue())
                               .setDefault(Either.class, EitherOfTwo.withB(Time.getCurrentTime()))
                               .testAllPublicStaticMethods(Pair.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        StringValue v1 = TestValues.newUuidValue();
        StringValue v2 = TestValues.newUuidValue();

        Pair<StringValue, StringValue> p1 = Pair.of(v1, v2);
        Pair<StringValue, StringValue> p1a = Pair.of(v1, v2);
        Pair<StringValue, StringValue> p2 = Pair.of(v2, v1);

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
        StringValue a = TestValues.newUuidValue();
        BoolValue b = BoolValue.of(true);

        Pair<StringValue, BoolValue> pair = Pair.of(a, b);

        assertEquals(a, pair.getA());
        assertEquals(b, pair.getB());
    }

    @Nested
    @DisplayName("allow optional B")
    class AllowOptionalB {

        @Test
        @DisplayName("absent")
        void absent() {
            StringValue a = TestValues.newUuidValue();
            Optional<BoolValue> b = Optional.empty();

            Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, null);

            assertEquals(a, pair.getA());
            assertEquals(b, pair.getB());
        }

        @Test
        @DisplayName("present")
        void present() {
            StringValue a = TestValues.newUuidValue();
            Optional<BoolValue> b = Optional.of(BoolValue.of(true));

            Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, b.get());

            assertEquals(a, pair.getA());
            assertEquals(b, pair.getB());
        }
    }

    @Test
    @DisplayName("return Empty for absent Optional in iterator")
    void getAbsentInIterator() {
        StringValue a = TestValues.newUuidValue();
        Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, null);

        Iterator<Message> iterator = pair.iterator();

        assertEquals(a, iterator.next());
        assertEquals(Empty.getDefaultInstance(), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        StringValue a = TestValues.newUuidValue();
        BoolValue b = BoolValue.of(true);

        reserializeAndAssert(Pair.of(a, b));
        reserializeAndAssert(Pair.withNullable(a, b));
        reserializeAndAssert(Pair.withNullable(a, null));

        reserializeAndAssert(Pair.withEither(a, EitherOfTwo.withA(Time.getCurrentTime())));
        reserializeAndAssert(Pair.withEither(a, EitherOfTwo.withB(TestValues.newUuidValue())));
    }
}
