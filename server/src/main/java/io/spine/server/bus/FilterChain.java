/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Optional;
import io.spine.core.IsSent;
import io.spine.core.MessageEnvelope;

import java.util.Deque;
import java.util.Queue;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * A {@link BusFilter} representing a chain of other bus filters.
 *
 * <p>The initial ordering of the filters is preserved. The filters are called sequentially in
 * the order of the initial {@link Queue}.
 *
 * <p>The {@link #close() close()} method closes all the underlying filters.
 *
 * @author Dmytro Dashenkov
 */
final class FilterChain<E extends MessageEnvelope<T>, T> implements BusFilter<E> {

    private final Deque<BusFilter<E>> chain;

    private volatile boolean closed;

    FilterChain(Queue<? extends BusFilter<E>> chain) {
        this.chain = newLinkedList(chain);
    }

    @Override
    public Optional<IsSent> accept(E envelope) {
        checkNotNull(envelope);
        for (BusFilter<E> filter : chain) {
            final Optional<IsSent> output = filter.accept(envelope);
            if (output.isPresent()) {
                return output;
            }
        }
        return Optional.absent();
    }

    /**
     * Closes all the filters in the chain and deletes them from the chain.
     *
     * <p>The filters are closed in the reversed order comparing to the invocation order.
     *
     * @throws IllegalStateException on a repetitive call
     * @throws Exception             if a filter throws an {@link Exception}
     */
    @Override
    public void close() throws Exception {
        checkState(!closed, "The Filter chain is already closed.");
        closed = true;
        while (!chain.isEmpty()) {
            final BusFilter<E> filter = chain.pollLast();
            filter.close();
        }
    }
}
