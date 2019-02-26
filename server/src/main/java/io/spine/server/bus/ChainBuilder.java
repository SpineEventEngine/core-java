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

package io.spine.server.bus;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.type.MessageEnvelope;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A builder for a chain of {@linkplain BusFilter bus filters}.
 *
 * @param <E> type of the filtered envelopes
 * @author Dmytro Dashenkov
 */
@CanIgnoreReturnValue
final class ChainBuilder<E extends MessageEnvelope<?, ?, ?>> {

    private final Deque<BusFilter<E>> filters = new ConcurrentLinkedDeque<>();

    /**
     * Appends the given {@link BusFilter} to the tail of the built chain.
     */
    ChainBuilder<E> append(BusFilter<E> filter) {
        checkNotNull(filter);
        filters.addLast(filter);
        return this;
    }

    /**
     * Prepends the given {@link BusFilter} to the head of the built chain.
     */
    ChainBuilder<E> prepend(BusFilter<E> filter) {
        checkNotNull(filter);
        filters.addFirst(filter);
        return this;
    }

    /**
     * Retrieves the filters already added to the builder.
     *
     * @return new {@link Deque} containing all the filters
     */
    Deque<BusFilter<E>> getFilters() {
        return new ConcurrentLinkedDeque<>(filters);
    }

    /**
     * Copies all the filters into a new instance of {@code ChainBuilder}.
     *
     * @return new {@code ChainBuilder} with the same filters in the same order as in the current
     */
    ChainBuilder<E> copy() {
        ChainBuilder<E> result = new ChainBuilder<>();
        filters.forEach(result::append);
        return result;
    }

    /**
     * Creates a new instance of {@link FilterChain} from the given filters.
     *
     * @return new {@link FilterChain}
     */
    FilterChain<E> build() {
        return new FilterChain<>(this);
    }
}
