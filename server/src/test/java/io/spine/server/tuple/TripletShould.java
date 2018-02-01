/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Optional;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.spine.test.TestValues;
import io.spine.time.Time;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("FieldNamingConvention") // short vars are OK for tuple tests.
public class TripletShould {

    private final StringValue a = TestValues.newUuidValue();
    private final BoolValue b = BoolValue.of(true);
    private final UInt32Value c = UInt32Value.newBuilder()
                                             .setValue(128)
                                             .build();

    private Triplet<StringValue, BoolValue, UInt32Value> triplet;

    @Before
    public void setUp() {
        triplet = Triplet.of(a, b, c);
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().setDefault(Message.class, TestValues.newUuidValue())
                               .setDefault(Optional.class, Optional.of(Time.getCurrentTime()))
                               .testAllPublicStaticMethods(Triplet.class);
    }

    @Test
    public void support_equality() {
        new EqualsTester().addEqualityGroup(Triplet.of(a, b, c), Triplet.of(a, b, c))

                          .addEqualityGroup(Triplet.withNullable(a, b, c))
                          .addEqualityGroup(Triplet.withNullable(a, b, null))

                          .addEqualityGroup(Triplet.withNullable2(a, b, c))
                          .addEqualityGroup(Triplet.withNullable2(a, b, null))
                          .addEqualityGroup(Triplet.withNullable2(a, null, c))
                          .addEqualityGroup(Triplet.withNullable2(a, null, null))
                          .testEquals();
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_default_A_value() {
        Triplet.of(StringValue.getDefaultInstance(), b, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_default_B_value() {
        Triplet.of(a, StringValue.getDefaultInstance(), c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_default_C_value() {
        Triplet.of(a, b, StringValue.getDefaultInstance());
    }

    @Test
    public void return_values() {
        assertEquals(a, triplet.getA());
        assertEquals(b, triplet.getB());
        assertEquals(c, triplet.getC());
    }

    @Test
    public void allow_optional_elements_present() {
        Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                Triplet.withNullable2(a, b, c);

        assertEquals(a, optTriplet.getA());
        assertEquals(Optional.of(b), optTriplet.getB());
        assertEquals(Optional.of(c), optTriplet.getC());
    }

    @Test
    public void allow_optional_elements_absent() {
        Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                Triplet.withNullable2(a, null, null);

        assertEquals(a, optTriplet.getA());
        assertEquals(Optional.absent(), optTriplet.getB());
        assertEquals(Optional.absent(), optTriplet.getC());
    }

    @Test
    public void return_Empty_for_absent_Optional_in_iterator() {
        Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                Triplet.withNullable2(a, null, null);

        final Iterator<Message> iterator = optTriplet.iterator();

        assertEquals(a, iterator.next());
        assertEquals(Empty.getDefaultInstance(), iterator.next());
        assertEquals(Empty.getDefaultInstance(), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_values_in_iteration() {
        final Iterator<Message> iterator = triplet.iterator();

        assertEquals(a, iterator.next());
        assertEquals(b, iterator.next());
        assertEquals(c, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void return_values_from_optional_in_iteration() {
        Triplet<StringValue, Optional<BoolValue>, Optional<UInt32Value>> optTriplet =
                Triplet.withNullable2(a, b, c);

        final Iterator<Message> iterator = optTriplet.iterator();

        assertEquals(a, iterator.next());
        assertEquals(b, iterator.next());
        assertEquals(c, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void serialize() {
        reserializeAndAssert(Triplet.of(a, b, c));

        reserializeAndAssert(Triplet.withNullable(a, b, c));
        reserializeAndAssert(Triplet.withNullable(a, b, null));

        reserializeAndAssert(Triplet.withNullable2(a, null, null));
        reserializeAndAssert(Triplet.withNullable2(a, b, null));
        reserializeAndAssert(Triplet.withNullable2(a, null, c));
    }
}
