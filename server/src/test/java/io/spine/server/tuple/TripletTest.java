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
import com.google.protobuf.UInt32Value;
import io.spine.base.Time;
import io.spine.testing.TestValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Optional;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"FieldNamingConvention", "InstanceVariableNamingConvention",
        /* Short vars are OK for tuple tests. */
        "DuplicateStringLiteralInspection" /* Common test display names. */,
        "ResultOfMethodCallIgnored" /* Methods are called to throw exception. */})
@DisplayName("Triplet should")
class TripletTest {

    private final StringValue a = TestValues.newUuidValue();
    private final BoolValue b = BoolValue.of(true);
    private final UInt32Value c = UInt32Value.newBuilder()
                                             .setValue(128)
                                             .build();

    private Triplet<StringValue, BoolValue, UInt32Value> triplet;

    @BeforeEach
    void setUp() {
        triplet = Triplet.of(a, b, c);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().setDefault(Message.class, TestValues.newUuidValue())
                               .setDefault(Optional.class, Optional.of(Time.getCurrentTime()))
                               .testAllPublicStaticMethods(Triplet.class);
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(Triplet.of(a, b, c), Triplet.of(a, b, c))

                          .addEqualityGroup(Triplet.withNullable(a, b, c))
                          .addEqualityGroup(Triplet.withNullable(a, b, null))

                          .addEqualityGroup(Triplet.withNullable2(a, b, c))
                          .addEqualityGroup(Triplet.withNullable2(a, b, null))
                          .addEqualityGroup(Triplet.withNullable2(a, null, c))
                          .addEqualityGroup(Triplet.withNullable2(a, null, null))
                          .testEquals();
    }

    @Nested
    @DisplayName("prohibit default value for")
    class ProhibitDefault {

        @Test
        @DisplayName("A")
        void a() {
            assertThrows(IllegalArgumentException.class,
                         () -> Triplet.of(StringValue.getDefaultInstance(), b, c));
        }

        @Test
        @DisplayName("B")
        void b() {
            assertThrows(IllegalArgumentException.class,
                         () -> Triplet.of(a, StringValue.getDefaultInstance(), c));
        }

        @Test
        @DisplayName("C")
        void c() {
            assertThrows(IllegalArgumentException.class,
                         () -> Triplet.of(a, b, StringValue.getDefaultInstance()));
        }
    }

    @Test
    @DisplayName("return values")
    void returnValues() {
        assertEquals(a, triplet.getA());
        assertEquals(b, triplet.getB());
        assertEquals(c, triplet.getC());
    }

    @Nested
    @DisplayName("allow optional elements")
    class AllowOptionalElements {

        @Test
        @DisplayName("present")
        void present() {
            Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                    Triplet.withNullable2(a, b, c);

            assertEquals(a, optTriplet.getA());
            assertEquals(Optional.of(b), optTriplet.getB());
            assertEquals(Optional.of(c), optTriplet.getC());
        }

        @Test
        @DisplayName("empty")
        void empty() {
            Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                    Triplet.withNullable2(a, null, null);

            assertEquals(a, optTriplet.getA());
            assertEquals(Optional.empty(), optTriplet.getB());
            assertEquals(Optional.empty(), optTriplet.getC());
        }
    }

    @Nested
    @DisplayName("in iterator, return")
    class ReturnInIterator {

        @Test
        @DisplayName("ordinary values")
        void ordinary() {
            Iterator<Message> iterator = triplet.iterator();

            assertEquals(a, iterator.next());
            assertEquals(b, iterator.next());
            assertEquals(c, iterator.next());
            assertFalse(iterator.hasNext());
        }

        @Test
        @DisplayName("Empty values")
        void empty() {
            Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                    Triplet.withNullable2(a, null, null);

            Iterator<Message> iterator = optTriplet.iterator();

            assertEquals(a, iterator.next());
            assertEquals(Empty.getDefaultInstance(), iterator.next());
            assertEquals(Empty.getDefaultInstance(), iterator.next());
            assertFalse(iterator.hasNext());
        }

        @Test
        @DisplayName("Optional values")
        void optional() {
            Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                    Triplet.withNullable2(a, b, c);

            Iterator<Message> iterator = optTriplet.iterator();

            assertEquals(a, iterator.next());
            assertEquals(b, iterator.next());
            assertEquals(c, iterator.next());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(Triplet.of(a, b, c));

        reserializeAndAssert(Triplet.withNullable(a, b, c));
        reserializeAndAssert(Triplet.withNullable(a, b, null));

        reserializeAndAssert(Triplet.withNullable2(a, null, null));
        reserializeAndAssert(Triplet.withNullable2(a, b, null));
        reserializeAndAssert(Triplet.withNullable2(a, null, c));
    }
}
