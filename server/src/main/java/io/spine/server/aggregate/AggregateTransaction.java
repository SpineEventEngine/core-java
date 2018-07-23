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
package io.spine.server.aggregate;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.entity.Transaction;
import io.spine.validate.ValidatingBuilder;

/**
 * A transaction, within which {@linkplain Aggregate Aggregate instances} are modified.
 *
 * @param <I> the type of aggregate IDs
 * @param <S> the type of aggregate state
 * @param <B> the type of a {@code ValidatingBuilder} for the aggregate state
 * @author Alex Tymchenko
 */
@Internal
public class AggregateTransaction<I,
                                  S extends Message,
                                  B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends Transaction<I, Aggregate<I, S, B>, S, B> {

    @VisibleForTesting
    AggregateTransaction(Aggregate<I, S, B> aggregate) {
        super(aggregate);
    }

    @VisibleForTesting
    protected AggregateTransaction(Aggregate<I, S, B> aggregate, S state, Version version) {
        super(aggregate, state, version);
    }

    /**
     * {@inheritDoc}
     *
     * <p>As long as {@linkplain EventApplier event applier method}
     * does not operate with {@linkplain EventContext event context}, this parameter is ignored.
     */
    @Override
    protected void dispatch(Aggregate aggregate, EventEnvelope event) {
        aggregate.invokeApplier(event.getMessage());
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is overridden to expose itself to repositories, state builders,
     * and test utilities.
     */
    @Override
    protected void commit() {
        super.commit();
    }

    /**
     * Creates a new transaction for a given {@code aggregate}.
     *
     * @param aggregate the {@code Aggregate} instance to start the transaction for.
     * @return the new transaction instance
     */
    @SuppressWarnings("unchecked")  // to avoid massive generic-related issues.
    static AggregateTransaction start(Aggregate aggregate) {
        AggregateTransaction tx = new AggregateTransaction(aggregate);
        return tx;
    }
}
