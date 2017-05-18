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
package org.spine3.server.entity;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import org.spine3.annotation.Internal;
import org.spine3.base.Version;
import org.spine3.server.entity.Transaction.Phase;
import org.spine3.validate.ValidatingBuilder;

/**
 * A common contract for the {@linkplain Transaction transaction} watchers.
 *
 * <p>Provides an ability to add callbacks to the transaction execution stages.
 *
 * @author Alex Tymchenko
 */
@Internal
public interface TransactionWatcher<I,
                                    E extends EventPlayingEntity<I, S, B>,
                                    S extends Message,
                                    B extends ValidatingBuilder<S, ? extends Message.Builder>> {

    /**
     * A callback invoked upon a {@linkplain Phase transaction phase} failure.
     *
     * <p>Optionally allows to return an exception, which is thrown after the callback execution,
     * effectively leading to the transaction rollback.
     *
     * @param e              the {@code Throwable} which caused the failure
     * @param failedPhase    the failed phase
     * @param previousPhases the phases executed within the same transaction before the failed one
     * @return an optional exception to throw
     */
    Optional<RuntimeException> phaseFailed(Exception e,
                                           Phase<I, E, S, B> failedPhase,
                                           Iterable<Phase<I, E, S, B>> previousPhases);

    /**
     * A callback invoked before applying a {@linkplain Phase transaction phase} .
     *
     * @param phase the phase which will be applied after this callback is invoked
     */
    void onBeforePhase(Phase<I, E, S, B> phase);

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
     * @param e              a {@code Throwable} caused the commit failure
     * @param entity         an entity modified within the transaction
     * @param state          a state to set to the entity during the commit
     * @param version        a version to set to the entity during the commit
     * @param lifecycleFlags a lifecycle flags to set to the entity during the commit
     */
    void onCommitFail(Exception e, E entity, S state,
                      Version version, LifecycleFlags lifecycleFlags);

    /**
     * An implementation of a {@code TransactionWatcher} which does not set any behavior for its
     * callbacks.
     *
     * <p>Using this type of watcher means that the phase failure is ignored and the
     * ongoing transaction execution is continued.
     */
    class SilentWitness<I,
                        E extends EventPlayingEntity<I, S, B>,
                        S extends Message,
                        B extends ValidatingBuilder<S, ? extends Message.Builder>>
            implements TransactionWatcher<I, E, S, B> {

        @Override
        public Optional<RuntimeException> phaseFailed(Exception e, Phase<I, E, S, B> failedPhase,
                                                      Iterable<Phase<I, E, S, B>> previousPhases) {
            return Optional.absent();
        }

        @Override
        public void onBeforePhase(Phase<I, E, S, B> failedPhase) {
            // do nothing.
        }

        @Override
        public void onAfterPhase(Phase<I, E, S, B> phase) {
            // do nothing.
        }

        @Override
        public void onBeforeCommit(E entity, S state, Version version,
                                   LifecycleFlags lifecycleFlags) {
            // do nothing.
        }

        @Override
        public void onCommitFail(Exception e, E entity, S state,
                                 Version version, LifecycleFlags lifecycleFlags) {
            // do nothing.
        }
    }

    /**
     * An implementation of a {@code TransactionWatcher} which requires propagation for each
     * transaction phase.
     */
    @SuppressWarnings("ProhibitedExceptionThrown")
    class PhasePropagationRequiredWatcher<I,
                                          E extends EventPlayingEntity<I, S, B>,
                                          S extends Message,
                                          B extends ValidatingBuilder<S, ? extends Message.Builder>>
            extends SilentWitness<I, E, S, B> {

        /**
         * Returns the exception, caused the failure of the phase, to be re-thrown.
         */
        @Override
        public Optional<RuntimeException> phaseFailed(Exception e, Phase<I, E, S, B> failedPhase,
                                                      Iterable<Phase<I, E, S, B>> previousPhases) {
            if (e instanceof RuntimeException) {
                return Optional.of((RuntimeException) e);
            }
            return Optional.of(new RuntimeException(e));
        }
    }
}
