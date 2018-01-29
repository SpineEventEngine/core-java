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
import io.spine.test.TestValues;
import io.spine.time.Time;
import org.junit.Test;

import java.util.Iterator;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("LocalVariableNamingConvention") // OK for tuple element values
public class PairShould {

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().setDefault(Message.class, TestValues.newUuidValue())
                               .testAllPublicStaticMethods(Pair.class);
    }

    @Test
    public void support_equality() {
        final StringValue v1 = TestValues.newUuidValue();
        final StringValue v2 = TestValues.newUuidValue();

        final Pair<StringValue, StringValue> p1 = Pair.of(v1, v2);
        final Pair<StringValue, StringValue> p1a = Pair.of(v1, v2);
        final Pair<StringValue, StringValue> p2 = Pair.of(v2, v1);

        new EqualsTester().addEqualityGroup(p1, p1a)
                          .addEqualityGroup(p2)
                          .testEquals();
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_default_A_value() {
        Pair.of(StringValue.getDefaultInstance(), BoolValue.of(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_default_B_value() {
        Pair.of(BoolValue.of(false), TestValues.newUuidValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_Empty_A() {
        Pair.of(Empty.getDefaultInstance(), BoolValue.of(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_Empty_B() {
        Pair.of(BoolValue.of(true), Empty.getDefaultInstance());
    }

    @Test
    public void return_values() {
        StringValue a = TestValues.newUuidValue();
        BoolValue b = BoolValue.of(true);

        Pair<StringValue, BoolValue> pair = Pair.of(a, b);

        assertEquals(a, pair.getA());
        assertEquals(b, pair.getB());
    }

    @Test
    public void allow_optional_B_absent() {
        StringValue a = TestValues.newUuidValue();
        Optional<BoolValue> b = Optional.absent();

        Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, null);

        assertEquals(a, pair.getA());
        assertEquals(b, pair.getB());
    }

    @Test
    public void allow_optional_B_present() {
        StringValue a = TestValues.newUuidValue();
        Optional<BoolValue> b = Optional.of(BoolValue.of(true));

        Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, b.get());

        assertEquals(a, pair.getA());
        assertEquals(b, pair.getB());
    }

    @Test
    public void return_Empty_for_absent_Optional_in_iterator() {
        StringValue a = TestValues.newUuidValue();
        Pair<StringValue, Optional<BoolValue>> pair = Pair.withNullable(a, null);

        final Iterator<Message> iterator = pair.iterator();

        assertEquals(a, iterator.next());
        assertEquals(Empty.getDefaultInstance(), iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void serialize() {
        StringValue a = TestValues.newUuidValue();
        BoolValue b = BoolValue.of(true);

        reserializeAndAssert(Pair.of(a, b));
        reserializeAndAssert(Pair.withNullable(a, b));
        reserializeAndAssert(Pair.withNullable(a, null));

        reserializeAndAssert(Pair.withEither(a, EitherOfTwo.withA(Time.getCurrentTime())));
        reserializeAndAssert(Pair.withEither(a, EitherOfTwo.withB(TestValues.newUuidValue())));
    }
}
