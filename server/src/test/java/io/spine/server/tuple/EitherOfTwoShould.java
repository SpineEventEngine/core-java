/*
 *  Copyright 2018, TeamDev Ltd. All rights reserved.
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
public class EitherOfTwoShould {

    private final StringValue a = TestValues.newUuidValue();
    private final Timestamp b = Time.getCurrentTime();

    private EitherOfTwo<StringValue, Timestamp> eitherWithA;
    private EitherOfTwo<StringValue, Timestamp> eitherWithB;

    @Before
    public void setUp() {
        eitherWithA = EitherOfTwo.withA(a);
        eitherWithB = EitherOfTwo.withB(b);
    }

    @Test
    public void support_equality() {
        new EqualsTester().addEqualityGroup(eitherWithA, EitherOfTwo.withA(a))
                          .addEqualityGroup(eitherWithB)
                          .testEquals();
    }

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().testAllPublicStaticMethods(EitherOfTwo.class);
    }

    @Test
    public void return_values() {
        assertEquals(a, eitherWithA.getA());
        assertEquals(b, eitherWithB.getB());
    }

    @Test
    public void return_value_index() {
        assertEquals(0, eitherWithA.getIndex());
        assertEquals(1, eitherWithB.getIndex());
    }

    @Test
    public void return_only_one_value_in_iteration() {
        final Iterator<Message> iteratorA = eitherWithA.iterator();

        assertEquals(a, iteratorA.next());
        assertFalse(iteratorA.hasNext());

        final Iterator<Message> iteratorB = eitherWithB.iterator();

        assertEquals(b, iteratorB.next());
        assertFalse(iteratorB.hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_B() {
        eitherWithA.getB();
    }

    @Test(expected = IllegalStateException.class)
    public void prohibit_obtaining_the_other_value_A() {
        eitherWithB.getA();
    }

    @Test
    public void serialize() {
        reserializeAndAssert(eitherWithA);
        reserializeAndAssert(eitherWithB);
    }
}
