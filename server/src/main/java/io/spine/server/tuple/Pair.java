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
import com.google.protobuf.Message;
import io.spine.server.tuple.Tuple.AValue;
import io.spine.server.tuple.Tuple.BValue;

import javax.annotation.Nullable;

/**
 * A pair of event messages.
 *
 * @param <A> the type of the first entry
 * @param <B> the type of the second entry
 *
 * @author Alexander Yevsyukov
 */
public final class Pair<A extends Message, B>
        extends Tuple
        implements AValue<A>, BValue<B> {

    private static final long serialVersionUID = 0L;

    private Pair(A a, B b) {
        super(a, b);
    }

    /**
     * Creates a new pair of values.
     */
    public static <A extends Message, B extends Message> Pair<A, B> of(A a, B b) {
        final Pair<A, B> result = new Pair<>(a, b);
        return result;
    }

    /**
     * Creates a pair with optionally present second value.
     */
    public static <A extends Message, B extends Message> Pair<A, Optional<B>>
        withNullable(A a, @Nullable B b) {
        final Pair<A, Optional<B>> result = new Pair<>(a, Optional.fromNullable(b));
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
}
