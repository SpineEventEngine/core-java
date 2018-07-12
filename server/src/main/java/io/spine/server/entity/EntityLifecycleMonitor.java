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
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.server.entity.Repository.Lifecycle;
import io.spine.validate.ValidatingBuilder;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * An implementation of {@link TransactionListener} which monitors the transaction flow and
 * triggers the {@linkplain Repository.Lifecycle entity lifecycle}.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class EntityLifecycleMonitor<I,
                                          E extends TransactionalEntity<I, S, B>,
                                          S extends Message,
                                          B extends ValidatingBuilder<S, ? extends Message.Builder>>
        implements TransactionListener<I, E, S, B> {

    private final Repository<I, ?> repository;
    private final List<Message> acknowledgedMessageIds;

    private EntityLifecycleMonitor(Repository<I, ?> repository) {
        this.repository = repository;
        this.acknowledgedMessageIds = newLinkedList();
    }

    /**
     * Creates a new instance of {@code EntityLifecycleMonitor}.
     *
     * @param repository the repository of the entity under transaction
     */
    public static
    <I,
     E extends TransactionalEntity<I, S, B>,
     S extends Message,
     B extends ValidatingBuilder<S, ? extends Message.Builder>>
    TransactionListener<I, E, S, B> newInstance(Repository<I, ?> repository) {
        checkNotNull(repository);
        return new EntityLifecycleMonitor<>(repository);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Memoizes the ID of the event applied by the given phase.
     */
    @Override
    public void onAfterPhase(Transaction.Phase<I, E, S, B> phase) {
        Message messageId = phase.eventId();
        acknowledgedMessageIds.add(messageId);
    }

    @Override
    public void onBeforeCommit(E entity, S state, Version version, LifecycleFlags lifecycleFlags) {
        // NOP.
    }

    /**
     * {@inheritDoc}
     *
     * <p>Notifies the {@link Lifecycle} of the entity state change.
     */
    @Override
    public void onAfterCommit(EntityRecordChange change) {
        Set<Message> messageIds = copyOf(acknowledgedMessageIds);
        I id = Identifier.unpack(change.getPreviousValue()
                                       .getEntityId());
        Lifecycle lifecycle = repository.lifecycleOf(id);
        lifecycle.onStateChanged(change, messageIds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs the occurred {@link Throwable}.
     */
    @Override
    public void onTransactionFailed(Throwable t, E entity, S state, Version version,
                                    LifecycleFlags lifecycleFlags) {
        // NOP.
    }
}
