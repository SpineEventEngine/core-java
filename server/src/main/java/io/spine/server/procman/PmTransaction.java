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
package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.EventEnvelope;
import io.spine.core.Version;
import io.spine.server.entity.EntityVersioning;
import io.spine.server.entity.Transaction;
import io.spine.server.entity.TransactionListener;
import io.spine.validate.ValidatingBuilder;

/**
 * A transaction, within which {@linkplain ProcessManager ProcessManager instances} are modified.
 *
 * @param <I> the type of process manager IDs
 * @param <S> the type of process manager state
 * @param <B> the type of a {@code ValidatingBuilder} for the process manager state
 * @author Alex Tymchenko
 */
@Internal
public class PmTransaction<I,
                           S extends Message,
                           B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends Transaction<I, ProcessManager<I, S, B>, S, B> {

    @VisibleForTesting
    PmTransaction(ProcessManager<I, S, B> processManager) {
        super(processManager);
    }

    @VisibleForTesting
    PmTransaction(ProcessManager<I, S, B> processManager,
                  TransactionListener<I, ProcessManager<I, S, B>, S, B> listener) {
        super(processManager, listener);
    }


    @VisibleForTesting
    PmTransaction(ProcessManager<I, S, B> processManager, S state, Version version) {
        super(processManager, state, version);
    }

    //TODO:2018-05-23:alexander.yevsyukov: Check that we really can ignore events returned by
    // event reactor method of a ProcessManager. This looks like a bug.
    @SuppressWarnings("CheckReturnValue")
    @Override
    protected void dispatch(ProcessManager processManager, EventEnvelope event) {
        processManager.dispatchEvent(event);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is overridden to expose itself to repositories, state builders,
     * and test utilitites.
     */
    @Override
    public void commit() {
        super.commit();
    }

    /**
     * Creates a new transaction for a given {@code ProcessManager}.
     *
     * @param  processManager the {@code ProcessManager} instance to start the transaction for
     * @return the new transaction instance
     */
    public static <I,
            S extends Message,
            B extends ValidatingBuilder<S, ? extends Message.Builder>>
    PmTransaction<I, S, B> start(ProcessManager<I, S, B> processManager) {
        final PmTransaction<I, S, B> tx = new PmTransaction<>(processManager);
        return tx;
    }

    /**
     * Creates a new transaction for a given {@code processManager} and sets the given {@code state}
     * and {@code version} as a starting point for the transaction.
     *
     * <p>Please note that the state and version specified are not applied to the given process
     * manager directly and require a {@linkplain Transaction#commit() transaction commit} in order
     * to be applied.
     *
     * @param  processManager
     *         the {@code ProcessManager} instance to start the transaction for
     * @param  state
     *         the starting state to set
     * @param  version
     *         the starting version to set
     * @return the new transaction instance
     */
    public static <I,
                   S extends Message,
                   B extends ValidatingBuilder<S, ? extends Message.Builder>,
                   P extends ProcessManager<I, S, B>>
    PmTransaction<I, S, B> startWith(P processManager, S state, Version version) {
        final PmTransaction<I, S, B> tx = new PmTransaction<>(processManager, state, version);
        return tx;
    }

    @Override
    protected EntityVersioning versioningStrategy() {
        return EntityVersioning.AUTO_INCREMENT;
    }
}
