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
package io.spine.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.entity.EntityVersioning;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.validate.ValidatingBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A transaction, within which {@linkplain Projection projection instances} are modified.
 *
 * @param <I> the type of projection IDs
 * @param <M> the type of projection state
 * @param <B> the type of a {@code ValidatingBuilder} for the projection state
 * @author Alex Tymchenko
 */
@Internal
public class ProjectionTransaction<I,
                                   M extends Message,
                                   B extends ValidatingBuilder<M, ? extends Message.Builder>>
        extends Transaction<I, Projection<I, M, B>, M, B> {

    @VisibleForTesting
    ProjectionTransaction(Projection<I, M, B> projection) {
        super(projection);
    }

    @VisibleForTesting
    ProjectionTransaction(Projection<I, M, B> projection,
                          TransactionListener<I, Projection<I, M, B>, M, B> listener) {
        super(projection, listener);
    }

    @VisibleForTesting
    ProjectionTransaction(Projection<I, M, B> projection, M state, Version version) {
        super(projection, state, version);
    }

    @Override
    protected void dispatch(Projection projection, EventEnvelope event) {
        projection.apply(event.getMessage(), event.getEventContext());
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code ProjectionTransaction} uses the
     * {@link EntityVersioning#AUTO_INCREMENT AUTO_INCREMENT} strategy as the event versions are
     * irrelevant to the projection version.
     */
    @Override
    protected EntityVersioning versioningStrategy() {
        return EntityVersioning.AUTO_INCREMENT;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is overridden to expose itself to repositories, state builders, and test
     * utilities.
     */
    @Override
    public void commit() {
        super.commit();
    }

    /**
     * Creates a new transaction for a given {@code projection}.
     *
     * @param projection the {@code Projection} instance to start the transaction for.
     * @return the new transaction instance
     */
    protected static <I,
                      M extends Message,
                      B extends ValidatingBuilder<M, ? extends Message.Builder>>
    ProjectionTransaction<I, M, B> start(Projection<I, M, B> projection) {
        checkNotNull(projection);

        final ProjectionTransaction<I, M, B> tx = new ProjectionTransaction<>(projection);
        return tx;
    }

    /**
     * Creates a new transaction for a given {@code projection} and sets the given {@code state}
     * and {@code version} as a starting point for the transaction.
     *
     * <p>Please note that the state and version specified are not applied to the given projection
     * directly and require a {@linkplain Transaction#commit() transaction commit} in order
     * to be applied.
     *
     * @param projection  the {@code Projection} instance to start the transaction for.
     * @param state   the starting state to set
     * @param version the starting version to set
     * @return the new transaction instance
     */
    public static <I,
                   M extends Message,
                   B extends ValidatingBuilder<M, ? extends Message.Builder>>
    ProjectionTransaction<I, M, B> startWith(Projection<I, M, B> projection,
                                             M state,
                                             Version version) {
        checkNotNull(projection);
        checkNotNull(state);
        checkNotNull(version);

        final ProjectionTransaction<I, M, B> tx =
                new ProjectionTransaction<>(projection, state, version);
        return tx;
    }
}
