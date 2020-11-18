/*
 * Copyright 2020, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.server.tuple.Element.AValue;
import io.spine.server.tuple.Element.BValue;
import io.spine.server.tuple.Element.CValue;
import io.spine.server.tuple.Element.DValue;
import io.spine.server.tuple.Element.EValue;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static io.spine.server.tuple.Element.value;
import static io.spine.server.tuple.Values.isOptionalPresent;
import static java.util.Optional.ofNullable;

/**
 * A tuple of five elements.
 *
 * <p>The first element must be a non-default {@link Message}
 * and not {@link com.google.protobuf.Empty Empty}.
 *
 * <p>Other four can be {@code Message}, {@link java.util.Optional Optional} or
 * {@link Either}.
 *
 * @param <A>
 *         the type of the first element
 * @param <B>
 *         the type of the second element
 * @param <C>
 *         the type of the third element
 * @param <D>
 *         the type of the fourth element
 * @param <E>
 *         the type of the fifth element
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
        checkAllNotNullOrEmpty(a, b, c, d, e);
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
        checkAllNotNullOrEmpty(a, b, c, d);
        checkNotEmpty(e);
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
        checkAllNotNullOrEmpty(a, b, c);
        checkAllNotEmpty(e, d);
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
        checkAllNotNullOrEmpty(a, b);
        checkAllNotEmpty(c, d, e);
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
        checkNotNullOrEmpty(a);
        checkAllNotEmpty(b, c, d, e);
        Quintet<A, Optional<B>, Optional<C>, Optional<D>, Optional<E>> result =
                new Quintet<>(a, ofNullable(b), ofNullable(c), ofNullable(d), ofNullable(e));
        return result;
    }

    @Override
    public A getA() {
        return value(this, IndexOf.A);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns {@code true}.
     */
    @Override
    public boolean hasA() {
        return true;
    }

    @Override
    public B getB() {
        return value(this, IndexOf.B);
    }

    @Override
    public boolean hasB() {
        B value = getB();
        return isOptionalPresent(value);
    }

    @Override
    public C getC() {
        return value(this, IndexOf.C);
    }

    @Override
    public boolean hasC() {
        C value = getC();
        return isOptionalPresent(value);
    }

    @Override
    public D getD() {
        return value(this, IndexOf.D);
    }

    @Override
    public boolean hasD() {
        D value = getD();
        return isOptionalPresent(value);
    }

    @Override
    public E getE() {
        return value(this, IndexOf.E);
    }

    @Override
    public boolean hasE() {
        E value = getE();
        return isOptionalPresent(value);
    }

    @CanIgnoreReturnValue
    private static <M extends Message> M checkNotNullOrEmpty(M value) {
        return checkNotNullOrEmpty(Quintet.class, value);
    }

    @CanIgnoreReturnValue
    private static <M extends Message> @Nullable M checkNotEmpty(@Nullable M value) {
        return checkNotEmpty(Quintet.class, value);
    }

    @SuppressWarnings("OverloadedVarargsMethod") // to avoid repeated usage of this class name.
    private static void checkAllNotNullOrEmpty(Message... values) {
        checkAllNotNullOrEmpty(Quintet.class, values);
    }

    @SuppressWarnings("OverloadedVarargsMethod") // to avoid repeated usage of this class name.
    private static void checkAllNotEmpty(Message... values) {
        checkAllNotEmpty(Quintet.class, values);
    }
}
