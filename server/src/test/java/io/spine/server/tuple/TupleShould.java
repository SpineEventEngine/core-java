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

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;
import io.spine.test.TestValues;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings({"LocalVariableNamingConvention", "FieldNamingConvention"})
// OK for tuple entry values
public class TupleShould {

    private final StringValue a = TestValues.newUuidValue();
    private final EitherOfTwo<Timestamp, BoolValue> b = EitherOfTwo.withA(Time.getCurrentTime());

    private TTuple<StringValue, EitherOfTwo<Timestamp, BoolValue>> tuple;

    @Before
    public void setUp() {
        tuple = new TTuple<>(a, b);
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_Empty_values() {
        new TTuple<>(TestValues.newUuidValue(), Empty.getDefaultInstance());
    }

    @Test
    public void allow_Either_argument() {
        assertEquals(a, tuple.getA());
        assertEquals(b, tuple.getB());
    }

    @Test
    public void return_value_from_Either_on_iteration() {
        final Iterator<Message> iterator = tuple.iterator();

        assertEquals(a, iterator.next());
        assertEquals(b.getA(), iterator.next());
        assertFalse(iterator.hasNext());
    }

    /**
     * Descendant to test abstract base.
     */
    @SuppressWarnings("unchecked")
    private static class TTuple<A extends Message, B>
            extends Tuple
            implements AValue<A>, BValue<B> {

        private static final long serialVersionUID = 0L;

        TTuple(A a, B b) {
            super(a, b);
        }

        @Override
        public A getA() {
            return (A)get(0);
        }

        @Override
        public B getB() {
            return (B)get(1);
        }
    }
}
