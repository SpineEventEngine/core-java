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

import com.google.protobuf.Message;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;
import io.spine.server.tuple.Element.CValue;
import io.spine.server.tuple.Element.DValue;
import io.spine.server.tuple.Element.EValue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value which can be of one of five possible types.
 *
 * @param <A> the type of the first alternative
 * @param <B> the type of the second alternative
 * @param <C> the type of the third alternative
 * @param <D> the type of the fourth alternative
 * @param <E> the type of the fifth alternative
 */
public class EitherOf5<A extends Message,
                       B extends Message,
                       C extends Message,
                       D extends Message,
                       E extends Message>
        extends Either
        implements AValue<A>, BValue<B>, CValue<C>, DValue<D>, EValue<E> {

    private static final long serialVersionUID = 0L;

    private EitherOf5(Message value, int index) {
        super(value, index);
    }

    /**
     * Creates a new instance with {@code <A>} value.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    EitherOf5<A, B, C, D, E> withA(A a) {
        checkNotNull(a);
        EitherOf5<A, B, C, D, E> result = new EitherOf5<>(a, 0);
        return result;
    }

    /**
     * Creates a new instance with {@code <B>} value.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    EitherOf5<A, B, C, D, E> withB(B b) {
        checkNotNull(b);
        EitherOf5<A, B, C, D, E> result = new EitherOf5<>(b, 1);
        return result;
    }

    /**
     * Creates a new instance with {@code <C>} value.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    EitherOf5<A, B, C, D, E> withC(C c) {
        checkNotNull(c);
        EitherOf5<A, B, C, D, E> result = new EitherOf5<>(c, 2);
        return result;
    }

    /**
     * Creates a new instance with {@code <D>} value.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    EitherOf5<A, B, C, D, E> withD(D d) {
        checkNotNull(d);
        EitherOf5<A, B, C, D, E> result = new EitherOf5<>(d, 3);
        return result;
    }

    /**
     * Creates a new instance with {@code <E>} value.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    EitherOf5<A, B, C, D, E> withE(E e) {
        checkNotNull(e);
        EitherOf5<A, B, C, D, E> result = new EitherOf5<>(e, 4);
        return result;
    }

    /**
     * Obtains the value of the first alternative.
     *
     * @throws IllegalStateException if a value of another type is stored instead.
     * @return the stored value.
     */
    @Override
    public A getA() {
        return get(this, 0);
    }

    /**
     * Obtains the value of the second alternative.
     *
     * @throws IllegalStateException if a value of another type is stored instead.
     * @return the stored value.
     */
    @Override
    public B getB() {
        return get(this, 1);
    }

    /**
     * Obtains the value of the third alternative.
     *
     * @throws IllegalStateException if a value of another type is stored instead.
     * @return the stored value.
     */
    @Override
    public C getC() {
        return get(this, 2);
    }

    /**
     * Obtains the value of the third alternative.
     *
     * @throws IllegalStateException if a value of another type is stored instead.
     * @return the stored value.
     */
    @Override
    public D getD() {
        return get(this, 3);
    }

    @Override
    public E getE() {
        return get(this, 4);
    }
}
