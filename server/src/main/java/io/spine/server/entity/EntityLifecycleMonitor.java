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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.validate.ValidatingBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * An implementation of {@link TransactionListener} which monitors the transaction flow and
 * triggers the {@link EntityLifecycle}.
 *
 * <p>On a successful {@link Phase}, memoizes the ID of the applied message. After a successful
 * transaction commit, passes the memoized IDs to the {@link EntityLifecycle} of the entity under
 * transaction, along with the information about the mutations performed under the transaction - an
 * {@link EntityRecordChange}.
 *
 * <p>An instance of this {@link TransactionListener} is meant to serve for a single
 * {@link Transaction}. When trying to reuse a listener for multiple transactions,
 * an {@link IllegalStateException} is thrown.
 *
 * @param <I>
 *         ID type of the entity under transaction
 * @param <E>
 *         type of entity under transaction
 * @param <S>
 *         state type of the entity under transaction
 * @param <B>
 *         type of {@link ValidatingBuilder} of {@code S}
 */
@Internal
public final class EntityLifecycleMonitor<I,
                                          E extends TransactionalEntity<I, S, B>,
                                          S extends Message,
                                          B extends ValidatingBuilder<S, ? extends Message.Builder>>
        implements TransactionListener<I, E, S, B> {

    private final Repository<I, ?> repository;
    private final List<Message> acknowledgedMessageIds;

    private @MonotonicNonNull I entityId;

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
     * <p>Memoizes the ID of the event applied by the given phase. The received event IDs will be
     * reported to the {@link EntityLifecycle} after a successful commit.
     */
    @Override
    public void onAfterPhase(Phase<I, ?> phase) {
        checkSameEntity(phase.getEntityId());
        Message messageId = phase.getMessageId();
        acknowledgedMessageIds.add(messageId);
    }

    @Override
    public void onBeforeCommit(E entity, S state, Version version, LifecycleFlags lifecycleFlags) {
        // NOP.
    }

    /**
     * {@inheritDoc}
     *
     * <p>Notifies the {@link EntityLifecycle} of the entity state change.
     */
    @Override
    public void onAfterCommit(EntityRecordChange change) {
        Set<Message> messageIds = copyOf(acknowledgedMessageIds);
        Any newEntityId = change.getPreviousValue()
                                .getEntityId();
        I id = Identifier.unpack(newEntityId, repository.getIdClass());
        repository.lifecycleOf(id)
                  .onStateChanged(change, messageIds);
    }

    @Override
    public void onTransactionFailed(Throwable t, E entity, S state, Version version,
                                    LifecycleFlags lifecycleFlags) {
        // NOP.
    }

    /**
     * Ensures that the given entity is the same that was given the previous time.
     *
     * <p>An instance of {@code EntityLifecycleMonitor} should not be reused for multiple entity
     * transactions. Thus, if the given entity is not the same as seen before,
     * an {@link IllegalStateException} is thrown
     *
     * @param entityId
     *         the ID of the entity to check
     * @throws IllegalStateException if the check fails
     */
    private void checkSameEntity(I entityId) throws IllegalStateException {
        if (this.entityId == null) {
            this.entityId = entityId;
        } else {
            checkState(this.entityId.equals(entityId),
                       "Tried to reuse an instance of %s for multiple transactions.",
                       EntityLifecycleMonitor.class.getSimpleName());
        }
    }
}
