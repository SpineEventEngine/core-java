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
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("`EitherOfTwo` should")
@SuppressWarnings({"FieldNamingConvention", "InstanceVariableNamingConvention",
        /* Short vars are OK for tuple tests. */
        "DuplicateStringLiteralInspection" /* Common test display names. */,
        "ResultOfMethodCallIgnored" /* Methods are called to throw exception. */})
class EitherOf2Test {

    private final StringValue a = TestValues.newUuidValue();
    private final Timestamp b = Time.currentTime();

    private EitherOf2<StringValue, Timestamp> eitherWithA;
    private EitherOf2<StringValue, Timestamp> eitherWithB;

    @BeforeEach
    void setUp() {
        eitherWithA = EitherOf2.withA(a);
        eitherWithB = EitherOf2.withB(b);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOf2.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOf2.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .testEquals();
    }

    @Test
    @DisplayName("return values")
    void returnValues() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
    }

    @Test
    @DisplayName("tell if the values are set")
    void reportHasValues() {
        assertTrue(eitherWithA.hasA());
        assertFalse(eitherWithA.hasB());

        assertTrue(eitherWithB.hasB());
        assertFalse(eitherWithB.hasA());
    }

    @Test
    @DisplayName("return value index")
    void returnValueIndex() {
        assertEquals(0, eitherWithA.index());
        assertEquals(1, eitherWithB.index());
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
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
    }

    @Test
    @DisplayName("prohibit obtaining B when A is set")
    void notGetBForA() {
        assertThrows(IllegalStateException.class, () -> eitherWithA.getB());
    }

    @Test
    @DisplayName("prohibit obtaining A when B is set")
    void notGetAForB() {
        assertThrows(IllegalStateException.class, () -> eitherWithB.getA());
    }
}
