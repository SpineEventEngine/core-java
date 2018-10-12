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

import com.google.protobuf.Message;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value which can be one of two possible types.
 *
 * @param <A> the type of the first alternative
 * @param <B> the type of the second alternative
 */
public final class EitherOf2<A extends Message, B extends Message>
        extends Either
        implements AValue<A>, BValue<B> {

    private static final long serialVersionUID = 0L;

    private EitherOf2(Message value, int index) {
        super(value, index);
    }

    /**
     * Creates a new instance with {@code <A>} value.
     */
    public static <A extends Message, B extends Message> EitherOf2<A, B> withA(A a) {
        checkNotNull(a);
        EitherOf2<A, B> result = new EitherOf2<>(a, 0);
        return result;
    }

    /**
     * Creates a new instance with {@code <B>} value.
     */
    public static <A extends Message, B extends Message> EitherOf2<A, B> withB(B b) {
        checkNotNull(b);
        EitherOf2<A, B> result = new EitherOf2<>(b, 1);
        return result;
    }

    /**
     * Obtains the value of the first alternative.
     *
     * @throws IllegalStateException if the {@code <B>} value is stored instead.
     * @return the stored value.
     */
    @Override
    public A getA() {
        return get(this, 0);
    }

    /**
     * Obtains the value of the second alternative.
     *
     * @throws IllegalStateException if the {@code <A>} value is stored instead.
     * @return the stored value.
     */
    @Override
    public B getB() {
        return get(this, 1);
    }
}
