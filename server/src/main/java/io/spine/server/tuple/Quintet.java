/*
 * Copyright 2019, TeamDev. All rights reserved.
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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static io.spine.server.tuple.Element.value;
import static java.util.Optional.ofNullable;

/**
 * A tuple of five elements.
 *
 * <p>The first element must be a non-default {@link Message}
 * (and not {@link com.google.protobuf.Empty Empty}).
 *
 * <p>Other four can be {@code Message}, {@link java.util.Optional Optional} or
 * {@link Either}.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param <D> the type of the fourth element
 * @param <E> the type of the fifth element
 *
 * @author Alexander Yevsyukov
 */
public final class Quintet<A extends Message, B, C, D, E>
    extends Tuple
    implements AValue<A>, BValue<B>, CValue<C>, DValue<D>, EValue<E> {

    private static final long serialVersionUID = 0L;

    private Quintet(A a, B b, C c, D d, E e) {
        super(a, b, c, d, e);
    }

    /**
     * Creates a quintet with all values present.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    Quintet<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
        checkAllNotNullOrEmpty(Quintet.class, a, b, c, d, e);
        Quintet<A, B, C, D, E> result = new Quintet<>(a, b, c, d, e);
        return result;
    }

    /**
     * Creates a quintet with one optional value.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    Quintet<A, B, C, D, Optional<E>>
    withNullable(A a, B b, C c, D d, @Nullable E e) {
        checkAllNotNullOrEmpty(Quintet.class, a, b, c, d);
        checkNotEmpty(Quintet.class, e);
        Quintet<A, B, C, D, Optional<E>> result = new Quintet<>(a, b, c, d, ofNullable(e));
        return result;
    }

    /**
     * Creates a quintet with two optional values.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    Quintet<A, B, C, Optional<D>, Optional<E>>
    withNullable2(A a, B b, C c, @Nullable D d, @Nullable E e) {
        checkAllNotNullOrEmpty(Quintet.class, a, b, c);
        checkAllNotEmpty(Quintet.class, e, d);
        Quintet<A, B, C, Optional<D>, Optional<E>> result =
                new Quintet<>(a, b, c, ofNullable(d), ofNullable(e));
        return result;
    }

    /**
     * Creates a quintet with three optional values.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    Quintet<A, B, Optional<C>, Optional<D>, Optional<E>>
    withNullable3(A a, B b, @Nullable C c, @Nullable D d, @Nullable E e) {
        checkAllNotNullOrEmpty(Quintet.class, a, b);
        checkAllNotEmpty(Quintet.class, c, d, e);
        Quintet<A, B, Optional<C>, Optional<D>, Optional<E>> result =
                new Quintet<>(a, b, ofNullable(c), ofNullable(d), ofNullable(e));
        return result;
    }

    /**
     * Creates a quintet with four optional values.
     */
    public static
    <A extends Message, B extends Message, C extends Message, D extends Message, E extends Message>
    Quintet<A, Optional<B>, Optional<C>, Optional<D>, Optional<E>>
    withNullable4(A a, @Nullable B b, @Nullable C c, @Nullable D d, @Nullable E e) {
        checkNotNullOrEmpty(Quintet.class, a);
        checkAllNotEmpty(Quintet.class, b, c, d, e);
        Quintet<A, Optional<B>, Optional<C>, Optional<D>, Optional<E>> result =
                new Quintet<>(a, ofNullable(b), ofNullable(c), ofNullable(d), ofNullable(e));
        return result;
    }

    @Override
    public A getA() {
        return value(this, 0);
    }

    @Override
    public B getB() {
        return value(this, 1);
    }

    @Override
    public C getC() {
        return value(this, 2);
    }

    @Override
    public D getD() {
        return value(this, 3);
    }

    @Override
    public E getE() {
        return value(this, 4);
    }
}
