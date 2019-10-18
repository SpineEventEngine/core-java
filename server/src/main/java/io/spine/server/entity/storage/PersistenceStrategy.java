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
 * <p>The interface methods do not accept {@code null}s and must not return
 * {@code null} with the exception of exception {@link PersistenceStrategyOfNull}.
 */
public interface PersistenceStrategy<T, R> extends Function<T, R> {

    /**
     * A convenience shortcut for {@link #apply(T)}.
     *
     * <p>Can be used when the object is known of being of type {@code T} but can't be cast to it
     * explicitly (e.g. in case of wildcard arguments).
     */
    @SuppressWarnings("unchecked") // See doc.
    default R applyTo(Object object) {
        checkNotNull(object);
        T value = (T) object;
        return apply(value);
    }

    static <T> PersistenceStrategy<T, T> identity() {
        return t -> t;
    }
}
