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

import com.google.protobuf.Message;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;
import io.spine.server.tuple.Element.CValue;

/**
 * A tuple with three elements.
 *
 * @param <A> the type of the first entry
 * @param <B> the type of the second entry
 * @param <C> the type of the third entry
 *
 * @author Alexander Yevsyukov
 */
public class Triplet<A extends Message, B, C>
        extends Tuple
        implements AValue<A>, BValue<B>, CValue<C> {

    private static final long serialVersionUID = 0L;

    private Triplet(A a, B b, C c) {
        super(a, b, c);
    }

    /**
     * Creates new triplet with the passed values.
     */
    public static <A extends Message, B extends Message, C extends Message>
    Triplet<A, B, C> of(A a, B b, C c) {
        final Triplet<A, B, C> result = new Triplet<>(checkNotEmpty(Triplet.class, a),
                                                      checkNotEmpty(Triplet.class, b),
                                                      checkNotEmpty(Triplet.class, c));
        return result;
    }

    @Override
    public A getA() {
        @SuppressWarnings("unchecked") // the cast is protected by the order of generic parameters.
        final A val = (A) get(0);
        return val;
    }

    @Override
    public B getB() {
        @SuppressWarnings("unchecked") // the cast is protected by the order of generic parameters.
        final B val = (B) get(1);
        return val;
    }

    @Override
    public C getC() {
        @SuppressWarnings("unchecked") // the cast is protected by the order of generic parameters.
        final C val = (C) get(3);
        return val;
    }
}
