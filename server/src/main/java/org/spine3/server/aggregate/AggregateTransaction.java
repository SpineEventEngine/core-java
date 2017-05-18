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
package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.server.entity.Transaction;
import org.spine3.validate.ValidatingBuilder;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;

/**
 * A transaction, within which {@linkplain Aggregate Aggregate instances} are modified.
 *
 * <p>The propagation of each transaction phase is NOT required for this type of transactions.
 * If a transaction phase fails, the next phase is executed anyway.
 *
 * @param <I> the type of aggregate IDs
 * @param <S> the type of aggregate state
 * @param <B> the type of a {@code ValidatingBuilder} for the aggregate state
 * @author Alex Tymchenko
 */
class AggregateTransaction<I,
                           S extends Message,
                           B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends Transaction<I, Aggregate<I, S, B>, S, B> {

    private AggregateTransaction(Aggregate<I, S, B> entity) {
        super(entity);
    }

    private AggregateTransaction(Aggregate<I, S, B> entity, S state, Version version) {
        super(entity, state, version);
    }

    @Override
    protected void invokeApplier(Aggregate entity,
                                 Message eventMessage,
                                 @Nullable EventContext ignored) throws InvocationTargetException {
        entity.invokeApplier(eventMessage);
    }

    @SuppressWarnings("RedundantMethodOverride") // overrides to expose to this package.
    @Override
    protected void commit() {
        super.commit();
    }

    /**
     * Creates a new transaction for a given {@code entity}.
     *
     * @param entity the entity to start the transaction for.
     * @return the new transaction instance
     */
    @SuppressWarnings("unchecked")  // to avoid massive generic-related issues.
    static AggregateTransaction start(Aggregate entity) {
        final AggregateTransaction tx = new AggregateTransaction(entity);
        return tx;
    }

    /**
     * Creates a new transaction for a given {@code entity} and sets the given {@code state}
     * and {@code version} as a starting point for the transaction.
     *
     * <p>Please note that the state and version specified are not applied to the given entity
     * directly and require a {@linkplain Transaction#commit() transaction commit} in order
     * to be applied.
     *
     * @param entity  the entity to start the transaction for.
     * @param state   the starting state to set
     * @param version the starting version to set
     * @return the new transaction instance
     */
    @SuppressWarnings("unchecked")  // to avoid massive generic-related issues.
    static AggregateTransaction startWith(Aggregate entity, Message state, Version version) {
        final AggregateTransaction tx = new AggregateTransaction(entity, state, version);
        return tx;
    }
}
