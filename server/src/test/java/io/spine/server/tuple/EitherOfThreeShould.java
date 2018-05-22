/*
 *  Copyright 2018, TeamDev. All rights reserved.
 *
 *  Redistribution and use in source and/or binary forms, with or without
 *  modification, must retain the above copyright notice and the following
 *  disclaimer.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.tuple;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.UInt32Value;
import io.spine.base.Time;
import io.spine.test.TestValues;
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
public class EitherOfThreeShould {

    private final StringValue a = TestValues.newUuidValue();
    private final UInt32Value b = UInt32Value.newBuilder()
                                             .setValue(42)
                                             .build();
    private final Timestamp c = Time.getCurrentTime();

    private EitherOfThree<StringValue, UInt32Value, Timestamp> eitherWithA;
    private EitherOfThree<StringValue, UInt32Value, Timestamp> eitherWithB;
    private EitherOfThree<StringValue, UInt32Value, Timestamp> eitherWithC;

    @Before
    public void setUp() {
        eitherWithA = EitherOfThree.withA(a);
        eitherWithB = EitherOfThree.withB(b);
        eitherWithC = EitherOfThree.withC(c);
    }

    @Test
    public void support_equality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOfThree.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .addEqualityGroup(eitherWithC)
                          .testEquals();
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOfThree.class);
    }

    @Test
    public void return_values() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
        assertEquals(c, eitherWithC.getC());
    }

    @Test
    public void return_value_index() {
        assertEquals(0, eitherWithA.getIndex());
        assertEquals(1, eitherWithB.getIndex());
        assertEquals(2, eitherWithC.getIndex());
    }

    @Test
    public void return_only_one_value_in_iteration() {
        final Iterator<Message> iteratorA = eitherWithA.iterator();

        assertEquals(a, iteratorA.next());
        assertFalse(iteratorA.hasNext());

        final Iterator<Message> iteratorB = eitherWithB.iterator();

        assertEquals(b, iteratorB.next());
        assertFalse(iteratorB.hasNext());

        final Iterator<Message> iteratorC = eitherWithC.iterator();

        assertEquals(c, iteratorC.next());
        assertFalse(iteratorC.hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A_B() {
        eitherWithA.getB();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A_C() {
        eitherWithA.getC();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B_A() {
        eitherWithB.getA();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B_C() {
        eitherWithB.getC();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_C_A() {
        eitherWithC.getA();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_C_B() {
        eitherWithC.getB();
    }

    @Test
    public void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
        reserializeAndAssert(eitherWithC);
    }
}
