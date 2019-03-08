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

package io.spine.server.tuple;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.base.Time;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"FieldNamingConvention", "InstanceVariableNamingConvention",
        /* Short vars are OK for tuple tests. */
        "DuplicateStringLiteralInspection" /* Common test display names. */,
        "ResultOfMethodCallIgnored" /* Methods are called to throw exception. */})
@DisplayName("EitherOfThree should")
class EitherOf3Test {

    private final StringValue a = TestValues.newUuidValue();
    private final UInt32Value b = UInt32Value.newBuilder()
                                             .setValue(42)
                                             .build();
    private final Timestamp c = Time.currentTime();

    private EitherOf3<StringValue, UInt32Value, Timestamp> eitherWithA;
    private EitherOf3<StringValue, UInt32Value, Timestamp> eitherWithB;
    private EitherOf3<StringValue, UInt32Value, Timestamp> eitherWithC;

    @BeforeEach
    void setUp() {
        eitherWithA = EitherOf3.withA(a);
        eitherWithB = EitherOf3.withB(b);
        eitherWithC = EitherOf3.withC(c);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOf3.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOf3.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .addEqualityGroup(eitherWithC)
                          .testEquals();
    }

    @Test
    @DisplayName("return values")
    void returnValues() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
        assertEquals(c, eitherWithC.getC());
    }

    @Test
    @DisplayName("return value index")
    void returnValueIndex() {
        assertEquals(0, eitherWithA.getIndex());
        assertEquals(1, eitherWithB.getIndex());
        assertEquals(2, eitherWithC.getIndex());
    }

    @Test
    @DisplayName("return only one value in iteration")
    void provideProperIterator() {
        Iterator<Message> iteratorA = eitherWithA.iterator();

        assertEquals(a, iteratorA.next());
        assertFalse(iteratorA.hasNext());

        Iterator<Message> iteratorB = eitherWithB.iterator();

        assertEquals(b, iteratorB.next());
        assertFalse(iteratorB.hasNext());

        Iterator<Message> iteratorC = eitherWithC.iterator();

        assertEquals(c, iteratorC.next());
        assertFalse(iteratorC.hasNext());
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
        reserializeAndAssert(eitherWithC);
    }

    @Nested
    @DisplayName("when A is set, prohibit obtaining")
    class ProhibitObtainingForA {

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalStateException.class, () -> eitherWithA.getB());
        }

        @Test
        @DisplayName("C")
        void c() {
            assertThrows(IllegalStateException.class, () -> eitherWithA.getC());
        }
    }

    @Nested
    @DisplayName("when B is set, prohibit obtaining")
    class ProhibitObtainingForB {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalStateException.class, () -> eitherWithB.getA());
        }

        @Test
        @DisplayName("C")
        void c() {
            assertThrows(IllegalStateException.class, () -> eitherWithB.getC());
        }
    }

    @Nested
    @DisplayName("when C is set, prohibit obtaining")
    class ProhibitObtainingForC {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalStateException.class, () -> eitherWithC.getA());
        }

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalStateException.class, () -> eitherWithC.getB());
        }
    }
}
