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
package org.spine3.server.projection;

import com.google.protobuf.Message;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.server.entity.Transaction;
import org.spine3.server.entity.TransactionWatcher;
import org.spine3.server.entity.TransactionWatcher.PhasePropagationRequiredWatcher;
import org.spine3.validate.ValidatingBuilder;

import java.lang.reflect.InvocationTargetException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A transaction, within which {@linkplain Projection projection instances} are modified.
 *
 * <p>The propagation of each transaction phase is required for this type of transactions.
 * If a transaction phase fails, the failure exception is rethrown.
 *
 * @param <I> the type of projection IDs
 * @param <M> the type of projection state
 * @param <B> the type of a {@code ValidatingBuilder} for the projection state
 * @author Alex Tymchenko
 */
class ProjectionTransaction<I,
                            M extends Message,
                            B extends ValidatingBuilder<M, ? extends Message.Builder>>
        extends Transaction<I, Projection<I, M, B>, M, B> {

    private final TransactionWatcher<I, Projection<I, M, B>, M, B> watcher =
            new PhasePropagationRequiredWatcher<>();

    private ProjectionTransaction(Projection<I, M, B> entity) {
        super(entity);
    }

    private ProjectionTransaction(Projection<I, M, B> entity, M state, Version version) {
        super(entity, state, version);
    }

    @Override
    protected void invokeApplier(Projection entity,
                                 Message eventMessage,
                                 EventContext context) throws InvocationTargetException {
        entity.apply(eventMessage, context);
    }

    @SuppressWarnings("RedundantMethodOverride") // overrides to expose to this package.
    @Override
    protected void commit() {
        super.commit();
    }

    @Override
    protected TransactionWatcher<I, Projection<I, M, B>, M, B> getWatcher() {
        return watcher;
    }

    /**
     * Creates a new transaction for a given {@code entity}.
     *
     * @param entity the entity to start the transaction for.
     * @return the new transaction instance
     */
    static <I,
            M extends Message,
            B extends ValidatingBuilder<M, ? extends Message.Builder>>
    ProjectionTransaction<I, M, B> start(Projection<I, M, B> entity) {
        checkNotNull(entity);

        final ProjectionTransaction<I, M, B> tx = new ProjectionTransaction<>(entity);
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
    static <I,
            M extends Message,
            B extends ValidatingBuilder<M, ? extends Message.Builder>>
    ProjectionTransaction<I, M, B> startWith(Projection<I, M, B> entity, M state, Version version) {
        checkNotNull(entity);
        checkNotNull(state);
        checkNotNull(version);

        final ProjectionTransaction<I, M, B> tx = new ProjectionTransaction<>(entity,
                                                                              state,
                                                                              version);
        return tx;
    }
}
