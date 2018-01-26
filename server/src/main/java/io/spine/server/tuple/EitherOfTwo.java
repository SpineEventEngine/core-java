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

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value which can be one of two possible types.
 *
 * @param <A> the type for the first alternative
 * @param <B> the type of the second alternative
 * @author Alexander Yevsyukov
 */
public final class EitherOfTwo<A extends Message, B extends Message>
        extends Either
        implements AValue<A>, BValue<B> {

    private static final long serialVersionUID = 0L;

    private EitherOfTwo(@Nullable A a, @Nullable B b) {
        super(a == null ? Optional.absent() : a,
              b == null ? Optional.absent() : b);
        if (a == null && b == null) {
            throw new NullPointerException("Both values cannot be null");
        }
    }

    /**
     * Creates instance with {@code <A>} value.
     */
    public static <A extends Message, B extends Message> EitherOfTwo<A, B> withA(A a) {
        checkNotNull(a);
        final EitherOfTwo<A, B> result = new EitherOfTwo<>(a, null);
        return result;
    }

    /**
     * Creates instance with {@code <B>} value.
     */
    public static <A extends Message, B extends Message> EitherOfTwo<A, B> withB(B b) {
        checkNotNull(b);
        final EitherOfTwo<A, B> result = new EitherOfTwo<>(null, b);
        return result;
    }

    @Override
    public A getA() {
        return get(this, 0);
    }

    @Override
    public B getB() {
        return get(this, 1);
    }
}
