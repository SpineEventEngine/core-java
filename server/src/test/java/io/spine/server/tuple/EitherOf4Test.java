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
@DisplayName("EitherOfFour should")
class EitherOf4Test {

    private final StringValue a = TestValues.newUuidValue();
    private final BoolValue b = BoolValue.of(true);
    private final Timestamp c = Time.getCurrentTime();
    private final UInt32Value d = UInt32Value.newBuilder()
                                             .setValue(1024)
                                             .build();

    private EitherOf4<StringValue, BoolValue, Timestamp, UInt32Value> eitherWithA;
    private EitherOf4<StringValue, BoolValue, Timestamp, UInt32Value> eitherWithB;
    private EitherOf4<StringValue, BoolValue, Timestamp, UInt32Value> eitherWithC;
    private EitherOf4<StringValue, BoolValue, Timestamp, UInt32Value> eitherWithD;

    @BeforeEach
    void setUp() {
        eitherWithA = EitherOf4.withA(a);
        eitherWithB = EitherOf4.withB(b);
        eitherWithC = EitherOf4.withC(c);
        eitherWithD = EitherOf4.withD(d);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOf4.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOf4.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .addEqualityGroup(eitherWithC)
                          .addEqualityGroup(eitherWithD)
                          .testEquals();
    }

    @Test
    @DisplayName("return values")
    void returnValues() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
        assertEquals(c, eitherWithC.getC());
        assertEquals(d, eitherWithD.getD());
    }

    @Test
    @DisplayName("return value index")
    void returnValueIndex() {
        assertEquals(0, eitherWithA.getIndex());
        assertEquals(1, eitherWithB.getIndex());
        assertEquals(2, eitherWithC.getIndex());
        assertEquals(3, eitherWithD.getIndex());
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

        Iterator<Message> iteratorD = eitherWithD.iterator();

        assertEquals(d, iteratorD.next());
        assertFalse(iteratorD.hasNext());
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
        reserializeAndAssert(eitherWithC);
        reserializeAndAssert(eitherWithD);
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

        @Test
        @DisplayName("D")
        void d() {
            assertThrows(IllegalStateException.class, () -> eitherWithA.getD());
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

        @Test
        @DisplayName("D")
        void d() {
            assertThrows(IllegalStateException.class, () -> eitherWithB.getD());
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

        @Test
        @DisplayName("D")
        void d() {
            assertThrows(IllegalStateException.class, () -> eitherWithC.getD());
        }
    }

    @Nested
    @DisplayName("when D is set, prohibit obtaining")
    class ProhibitObtainingForD {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalStateException.class, () -> eitherWithD.getA());
        }

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalStateException.class, () -> eitherWithD.getB());
        }

        @Test
        @DisplayName("C")
        void c() {
            assertThrows(IllegalStateException.class, () -> eitherWithD.getC());
        }
    }
}
