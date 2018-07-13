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
package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Version;
import io.spine.server.entity.Transaction.Phase;
import io.spine.validate.ValidatingBuilder;

/**
 * A common contract for the {@linkplain Transaction transaction} listeners.
 *
 * <p>Provides an ability to add callbacks to the transaction execution stages.
 *
 * @param <I> ID type of the entity under transaction
 * @param <E> type of entity under transaction
 * @param <S> state type of the entity under transaction
 * @param <B> type of {@link ValidatingBuilder} of {@code S}
 * @author Alex Tymchenko
 */
@Internal
public interface TransactionListener<I,
                                     E extends TransactionalEntity<I, S, B>,
                                     S extends Message,
                                     B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    /**
     * A callback invoked after applying a {@linkplain Phase transaction phase}.
     *
     * <p>This callback is invoked for both successfully applied and failed phases.
     *
     * @param phase the phase which was applied before this callback is invoked
     */
    void onAfterPhase(Phase<I, E, S, B> phase);

    /**
     * A callback invoked before committing the transaction.
     *
     * @param entity         an entity modified within the transaction
     * @param state          a state to set to the entity during the commit
     * @param version        a version to set to the entity during the commit
     * @param lifecycleFlags a lifecycle flags to set to the entity during the commit
     */
    void onBeforeCommit(E entity, S state, Version version, LifecycleFlags lifecycleFlags);

    /**
     * A callback invoked if the commit has failed.
     *
     * @param t              a {@code Throwable} caused the commit failure
     * @param entity         an entity modified within the transaction
     * @param state          a state to set to the entity during the commit
     * @param version        a version to set to the entity during the commit
     * @param lifecycleFlags a lifecycle flags to set to the entity during the commit
     */
    void onTransactionFailed(Throwable t, E entity, S state,
                             Version version, LifecycleFlags lifecycleFlags);

    /**
     * A callback invoked after a successful commit.
     *
     * @param change the change of the entity under transaction
     */
    void onAfterCommit(EntityRecordChange change);

    /**
     * An implementation of a {@code TransactionListener} which does not set any behavior for its
     * callbacks.
     */
    class SilentWitness<I,
                        E extends TransactionalEntity<I, S, B>,
                        S extends Message,
                        B extends ValidatingBuilder<S, ? extends Message.Builder>>
            implements TransactionListener<I, E, S, B> {

        @Override
        public void onAfterPhase(Phase<I, E, S, B> phase) {
            // Do nothing.
        }

        @Override
        public void onBeforeCommit(E entity, S state, Version version,
                                   LifecycleFlags lifecycleFlags) {
            // Do nothing.
        }

        @Override
        public void onTransactionFailed(Throwable t, E entity, S state,
                                        Version version, LifecycleFlags lifecycleFlags) {
            // Do nothing.
        }

        @Override
        public void onAfterCommit(EntityRecordChange change) {
            // Do nothing.
        }
    }
}
