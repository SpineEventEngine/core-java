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

package io.spine.server.entity.storage;

import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A persistence strategy of an entity {@linkplain Column column}.
 *
 * <p>The {@link #apply(Object)} method generally does not accept {@code null}s and must not return
 * {@code null}, the only exception is {@link PersistenceStrategyOfNull}.
 */
public interface PersistenceStrategy<T, R> extends Function<T, R> {

    /**
     * The shortcut...
     */
    @SuppressWarnings("unchecked") // The object is implied to be of type`T`.
    default R applyTo(Object object) {
        checkNotNull(object);
        T value = (T) object;
        return apply(value);
    }

    static <T> PersistenceStrategy<T, T> identity() {
        return new Identity<>();
    }

    final class Identity<T> implements PersistenceStrategy<T, T> {

        @Override
        public T apply(T t) {
            return t;
        }
    }
}
