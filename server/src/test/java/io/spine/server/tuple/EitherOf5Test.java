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
import com.google.protobuf.FloatValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.base.Time;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`EitherOfFive` should")
@SuppressWarnings({"FieldNamingConvention", "InstanceVariableNamingConvention",
        /* Short vars are OK for tuple tests. */
        "DuplicateStringLiteralInspection" /* Common test display names. */,
        "ResultOfMethodCallIgnored" /* Methods are called to throw exception. */})
class EitherOf5Test {

    private final StringValue a = TestValues.newUuidValue();
    private final BoolValue b = BoolValue.of(true);
    private final Timestamp c = Time.currentTime();
    private final UInt32Value d = UInt32Value.newBuilder().setValue(512).build();
    private final FloatValue e = FloatValue.newBuilder().setValue(3.14159f).build();

    private EitherOf5<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithA;
    private EitherOf5<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithB;
    private EitherOf5<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithC;
    private EitherOf5<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithD;
    private EitherOf5<StringValue, BoolValue, Timestamp, UInt32Value, FloatValue> eitherWithE;

    @BeforeEach
    void setUp() {
        eitherWithA = EitherOf5.withA(a);
        eitherWithB = EitherOf5.withB(b);
        eitherWithC = EitherOf5.withC(c);
        eitherWithD = EitherOf5.withD(d);
        eitherWithE = EitherOf5.withE(e);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOf5.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOf5.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .addEqualityGroup(eitherWithC)
                          .addEqualityGroup(eitherWithD)
                          .addEqualityGroup(eitherWithE)
                          .testEquals();
    }

    @Test
    @DisplayName("return values")
    void returnValues() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
        assertEquals(c, eitherWithC.getC());
        assertEquals(d, eitherWithD.getD());
        assertEquals(e, eitherWithE.getE());
    }

    @Test
    @DisplayName("tell if the values are set")
    void reportHasValues() {
        assertTrue(eitherWithA.hasA());
        assertFalse(eitherWithA.hasB());
        assertFalse(eitherWithA.hasC());
        assertFalse(eitherWithA.hasD());
        assertFalse(eitherWithA.hasE());

        assertTrue(eitherWithB.hasB());
        assertFalse(eitherWithB.hasA());
        assertFalse(eitherWithB.hasC());
        assertFalse(eitherWithB.hasD());
        assertFalse(eitherWithB.hasE());

        assertTrue(eitherWithC.hasC());
        assertFalse(eitherWithC.hasA());
        assertFalse(eitherWithC.hasB());
        assertFalse(eitherWithC.hasD());
        assertFalse(eitherWithC.hasE());

        assertTrue(eitherWithD.hasD());
        assertFalse(eitherWithD.hasA());
        assertFalse(eitherWithD.hasB());
        assertFalse(eitherWithD.hasC());
        assertFalse(eitherWithD.hasE());

        assertTrue(eitherWithE.hasE());
        assertFalse(eitherWithE.hasA());
        assertFalse(eitherWithE.hasB());
        assertFalse(eitherWithE.hasC());
        assertFalse(eitherWithE.hasD());
    }

    @Test
    @DisplayName("return value index")
    void returnValueIndex() {
        assertEquals(0, eitherWithA.index());
        assertEquals(1, eitherWithB.index());
        assertEquals(2, eitherWithC.index());
        assertEquals(3, eitherWithD.index());
        assertEquals(4, eitherWithE.index());
    }

    @Test
    @DisplayName("return only one value in iteration")
    void provideProperIterator() {
        var iteratorA = eitherWithA.iterator();

        assertEquals(a, iteratorA.next());
        assertFalse(iteratorA.hasNext());

        var iteratorB = eitherWithB.iterator();

        assertEquals(b, iteratorB.next());
        assertFalse(iteratorB.hasNext());

        var iteratorC = eitherWithC.iterator();

        assertEquals(c, iteratorC.next());
        assertFalse(iteratorC.hasNext());

        var iteratorD = eitherWithD.iterator();

        assertEquals(d, iteratorD.next());
        assertFalse(iteratorD.hasNext());

        var iteratorE = eitherWithE.iterator();

        assertEquals(e, iteratorE.next());
        assertFalse(iteratorE.hasNext());
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
        reserializeAndAssert(eitherWithC);
        reserializeAndAssert(eitherWithD);
        reserializeAndAssert(eitherWithE);
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

        @Test
        @DisplayName("E")
        void e() {
            assertThrows(IllegalStateException.class, () -> eitherWithA.getE());
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

        @Test
        @DisplayName("E")
        void e() {
            assertThrows(IllegalStateException.class, () -> eitherWithB.getE());
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

        @Test
        @DisplayName("E")
        void e() {
            assertThrows(IllegalStateException.class, () -> eitherWithC.getE());
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

        @Test
        @DisplayName("E")
        void e() {
            assertThrows(IllegalStateException.class, () -> eitherWithD.getE());
        }
    }

    @Nested
    @DisplayName("when E is set, prohibit obtaining")
    class ProhibitObtainingForE {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalStateException.class, () -> eitherWithE.getA());
        }

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalStateException.class, () -> eitherWithE.getB());
        }

        @Test
        @DisplayName("C")
        void c() {
            assertThrows(IllegalStateException.class, () -> eitherWithE.getC());
        }

        @Test
        @DisplayName("D")
        void d() {
            assertThrows(IllegalStateException.class, () -> eitherWithE.getD());
        }
    }
}
