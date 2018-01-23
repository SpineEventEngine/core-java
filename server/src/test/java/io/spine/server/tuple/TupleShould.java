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

import com.google.protobuf.BoolValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("LocalVariableNamingConvention") // OK for tuple entry values
public class TupleShould {

    @Test
    public void allow_empty_value() {
        final Empty a = Empty.getDefaultInstance();
        final BoolValue b = BoolValue.of(true);

        final TTuple<Empty, BoolValue> tuple = new TTuple<>(a, b);

        assertEquals(a, tuple.getA());
        assertEquals(b, tuple.getB());
    }

    @Test(expected = IllegalArgumentException.class)
    public void prohibit_all_Empty_instances() {
        new TTuple<>(Empty.getDefaultInstance(), Empty.getDefaultInstance());
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
