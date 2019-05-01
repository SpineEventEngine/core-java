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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static io.spine.server.tuple.Element.value;
import static java.util.Optional.ofNullable;

/**
 * A tuple with three elements.
 *
 * <p>The first element must be a non-default {@link Message}
 * and not {@link com.google.protobuf.Empty Empty}.
 *
 * <p>Other two can be {@code Message}, {@link java.util.Optional Optional} or
 * {@link Either}.
 *
 * @param <A>
 *         the type of the first element
 * @param <B>
 *         the type of the second element
 * @param <C>
 *         the type of the third element
 */
public final class Triplet<A extends Message, B, C>
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
        checkAllNotNullOrEmpty(Triplet.class, a, b, c);
        Triplet<A, B, C> result = new Triplet<>(a, b, c);
        return result;
    }

    /**
     * Creates a triplet with the last element optional.
     */
    public static <A extends Message, B extends Message, C extends Message>
    Triplet<A, B, Optional<C>> withNullable(A a, B b, @Nullable C c) {
        checkAllNotNullOrEmpty(Triplet.class, a, b);
        checkNotEmpty(Triplet.class, c);
        Triplet<A, B, Optional<C>> result = new Triplet<>(a, b, ofNullable(c));
        return result;
    }

    /**
     * Creates a new triplet with optional second and third elements.
     */
    public static <A extends Message, B extends Message, C extends Message>
    Triplet<A, Optional<B>, Optional<C>> withNullable2(A a, @Nullable B b, @Nullable C c) {
        checkNotNullOrEmpty(Triplet.class, a);
        checkAllNotEmpty(Triplet.class, b, c);
        Triplet<A, Optional<B>, Optional<C>> result =
                new Triplet<>(a, ofNullable(b), ofNullable(c));
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
}
