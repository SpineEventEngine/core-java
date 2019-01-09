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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static io.spine.server.tuple.Element.value;
import static java.util.Optional.ofNullable;

/**
 * A tuple with four elements.
 *
 * <p>The first element must be a non-default {@link Message}
 * (and not {@link com.google.protobuf.Empty Empty}).
 *
 * <p>Other three can be {@code Message}, {@link java.util.Optional Optional} or
 * {@link Either}.
 *
 * @param <A> the type of the first element
 * @param <B> the type of the second element
 * @param <C> the type of the third element
 * @param <D> the type of the fourth element
 *
 * @author Alexander Yevsyukov
 */
public final class Quartet<A extends Message, B, C, D>
        extends Tuple
        implements AValue<A>, BValue<B>, CValue<C>, DValue<D> {

    private static final long serialVersionUID = 0L;

    private Quartet(A a, B b, C c, D d) {
        super(a, b, c, d);
    }

    /**
     * Creates a quartet with all values present.
     */
    public static <A extends Message, B extends Message, C extends Message, D extends Message>
    Quartet<A, B, C, D> of(A a, B b, C c, D d) {
        checkAllNotNullOrEmpty(Quartet.class, a, b, c, d);
        Quartet<A, B, C, D> result = new Quartet<>(a, b, c, d);
        return result;
    }

    /**
     * Creates a quartet with one optional value.
     */
    public static <A extends Message, B extends Message, C extends Message, D extends Message>
    Quartet<A, B, C, Optional<D>> withNullable(A a, B b, C c, @Nullable D d) {
        checkAllNotNullOrEmpty(Quartet.class, a, b, c);
        checkNotEmpty(Quartet.class, d);
        Quartet<A, B, C, Optional<D>> result = new Quartet<>(a, b, c, ofNullable(d));
        return result;
    }

    /**
     * Creates a quartet with two optional values.
     */
    public static <A extends Message, B extends Message, C extends Message, D extends Message>
    Quartet<A, B, Optional<C>, Optional<D>> withNullable2(A a, B b, @Nullable C c, @Nullable D d) {
        checkAllNotNullOrEmpty(Quartet.class, a, b);
        checkAllNotEmpty(Quartet.class, c, d);
        Quartet<A, B, Optional<C>, Optional<D>> result =
                new Quartet<>(a, b, ofNullable(c), ofNullable(d));
        return result;
    }

    /**
     * Creates a quartet with three optional values.
     */
    public static <A extends Message, B extends Message, C extends Message, D extends Message>
    Quartet<A, Optional<B>, Optional<C>, Optional<D>>
    withNullable3(A a, @Nullable B b, @Nullable C c, @Nullable D d) {
        checkNotNullOrEmpty(Quartet.class, a);
        checkAllNotEmpty(Quartet.class, b, c, d);
        Quartet<A, Optional<B>, Optional<C>, Optional<D>> result =
                new Quartet<>(a, ofNullable(b), ofNullable(c), ofNullable(d));
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
}
