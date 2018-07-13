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

package io.spine.server.bus;

import com.google.common.collect.Queues;
import io.spine.core.Ack;
import io.spine.core.MessageEnvelope;

import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link BusFilter} representing a chain of other bus filters.
 *
 * <p>The initial ordering of the filters is preserved. The filters are called sequentially in
 * the order direct of the initial {@link Deque}.
 *
 * <p>The {@link #close() close()} method closes all the underlying filters.
 *
 * @author Dmytro Dashenkov
 */
final class FilterChain<E extends MessageEnvelope<?, ?, ?>, F extends BusFilter<E>>
        implements BusFilter<E> {

    private final Deque<F> chain;

    private volatile boolean closed;

    FilterChain(Iterable<F> chain) {
        this.chain = Queues.newLinkedBlockingDeque(chain);
    }

    @Override
    public Optional<Ack> accept(E envelope) {
        checkNotNull(envelope);
        checkNotClosed();
        for (F filter : chain) {
            Optional<Ack> output = filter.accept(envelope);
            if (output.isPresent()) {
                return output;
            }
        }
        return Optional.empty();
    }

    /**
     * Closes all the filters in the chain and deletes them from the chain.
     *
     * <p>The filters are closed in the reversed order comparing to the invocation order.
     *
     * @throws IllegalStateException
     *         on a repetitive call
     * @throws Exception
     *         if a filter throws an {@link Exception}
     */
    @Override
    public void close() throws Exception {
        checkNotClosed();
        closed = true;
        Iterator<F> filters = chain.descendingIterator();
        while (filters.hasNext()) {
            F filter = filters.next();
            filter.close();
        }
    }

    private void checkNotClosed() {
        checkState(!closed, "The `FilterChain` is already closed.");
    }
}
