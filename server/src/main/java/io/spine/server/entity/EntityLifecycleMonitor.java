/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.core.MessageId;
import io.spine.core.MessageQualifier;
import io.spine.core.MessageWithContext;
import io.spine.logging.Logging;
import io.spine.validate.NonValidated;
import io.spine.validate.ValidationError;
import io.spine.validate.ValidationException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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
 */
@Internal
public final class EntityLifecycleMonitor<I> implements TransactionListener<I>, Logging {

    private static final MessageQualifier UNKNOWN_MESSAGE = MessageQualifier.getDefaultInstance();

    private final Repository<I, ?> repository;
    private final List<MessageId> acknowledgedMessageIds;
    private final I entityId;
    private @MonotonicNonNull MessageWithContext<?, ?, ?> lastMessage;

    private EntityLifecycleMonitor(Repository<I, ?> repository, I id) {
        this.repository = repository;
        this.acknowledgedMessageIds = newLinkedList();
        this.entityId = id;
    }

    /**
     * Creates a new instance of {@code EntityLifecycleMonitor}.
     *
     * @param repository the repository of the entity under transaction
     */
    public static <I> TransactionListener<I> newInstance(Repository<I, ?> repository, I id) {
        checkNotNull(repository);
        checkNotNull(id);
        return new EntityLifecycleMonitor<>(repository, id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Memoizes the dispatched message for diagnostics in case of a failure.
     */
    @Override
    public void onBeforePhase(Phase<I, ?> phase) {
        lastMessage = phase.message();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Memoizes the ID of the event applied by the given phase. The received event IDs will be
     * reported to the {@link EntityLifecycle} after a successful commit.
     */
    @Override
    public void onAfterPhase(Phase<I, ?> phase) {
        checkSameEntity(phase.entityId());
        MessageId messageId = phase.messageId();
        acknowledgedMessageIds.add(messageId);
    }

    @Override
    public void onBeforeCommit(@NonValidated EntityRecord entityRecord) {
        // NOP.
    }

    /**
     * {@inheritDoc}
     *
     * <p>Notifies the {@link EntityLifecycle} of the entity state change.
     */
    @Override
    public void onAfterCommit(EntityRecordChange change) {
        Set<MessageId> messageIds = ImmutableSet.copyOf(acknowledgedMessageIds);
        repository.lifecycleOf(entityId)
                  .onStateChanged(change, messageIds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Notifies the {@link EntityLifecycle} of the failure.
     */
    @Override
    public void onTransactionFailed(Throwable t, EntityRecord entityRecord) {
        Throwable cause = Throwables.getRootCause(t);
        if (cause instanceof ValidationException) {
            ValidationError error = ((ValidationException) cause).asValidationError();
            MessageQualifier causeMessage;
            MessageQualifier rootMessage;
            if (lastMessage != null) {
                causeMessage = lastMessage.qualifier();
                rootMessage = lastMessage.rootMessage();
            } else {
                causeMessage = UNKNOWN_MESSAGE;
                rootMessage = UNKNOWN_MESSAGE;
            }
            repository.lifecycleOf(entityId)
                      .onInvalidEntity(causeMessage,
                                       rootMessage,
                                       error,
                                       entityRecord.getVersion());
        }
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
        checkState(this.entityId.equals(entityId),
                   "Tried to reuse an instance of %s for multiple transactions.",
                   EntityLifecycleMonitor.class.getSimpleName());
    }
}
