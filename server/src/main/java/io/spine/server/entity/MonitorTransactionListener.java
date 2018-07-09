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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Lists.newLinkedList;

/**
 * @author Dmytro Dashenkov
 */
@Internal
public final class MonitorTransactionListener<I,
                                              E extends TransactionalEntity<I, S, B>,
                                              S extends Message,
                                              B extends ValidatingBuilder<S, ? extends Message.Builder>>
        implements TransactionListener<I, E, S, B> {

    private final Repository<I, ?> repository;
    private final List<Message> acknowledgedMessageIds;

    private MonitorTransactionListener(Repository<I, ?> repository) {
        this.repository = repository;
        this.acknowledgedMessageIds = newLinkedList();
    }

    public static
    <I,
     E extends TransactionalEntity<I, S, B>,
     S extends Message,
     B extends ValidatingBuilder<S, ? extends Message.Builder>>
    TransactionListener<I, E, S, B> instance(Repository<I, ?> repository) {
        checkNotNull(repository);
        return new MonitorTransactionListener<>(repository);
    }

    @Override
    public void onAfterPhase(Transaction.Phase<I, E, S, B> phase) {
        Message messageId = phase.eventId();
        acknowledgedMessageIds.add(messageId);
    }

    @Override
    public void onBeforeCommit(E entity, S state, Version version, LifecycleFlags lifecycleFlags) {

    }

    @Override
    public void onAfterCommit(EntityRecordChange change) {
        Set<Message> messageIds = copyOf(acknowledgedMessageIds);
        I id = Identifier.unpack(change.getPreviousValue()
                                       .getEntityId());
        Lifecycle lifecycle = repository.lifecycleOf(id);
        lifecycle.onStateChanged(change, messageIds);
    }

    @Override
    public void onTransactionFailed(Throwable t, E entity, S state, Version version,
                                    LifecycleFlags lifecycleFlags) {
        log().warn("Transaction failed.", t);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(MonitorTransactionListener.class);
    }
}
