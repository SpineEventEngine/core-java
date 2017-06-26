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

package io.spine.server.aggregate;

import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.tenant.TenantAwareOperation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Dispatches commands to aggregates of the associated {@code AggregateRepository}.
 *
 * <p>Loading and storing an aggregate is a tenant-sensitive operation,
 * which depends on the tenant ID of the command we dispatch.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by the parent repository
 * @author Alexander Yevsyukov
 */
class AggregateCommandEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends TenantAwareOperation {

    private final AggregateRepository<I, A> repository;
    private final CommandEnvelope command;

    @Nullable
    private A aggregate;

    static <I, A extends Aggregate<I, ?, ?>>
            AggregateCommandEndpoint<I, A> createFor(AggregateRepository<I, A> repository,
                                                     CommandEnvelope command) {
        return new AggregateCommandEndpoint<>(repository, command);
    }

    private AggregateCommandEndpoint(AggregateRepository<I, A> repository,
                                     CommandEnvelope command) {
        super(command.getTenantId());
        this.repository = repository;
        this.command = command;
    }

    @Override
    public void run() {
        aggregate = receive(command);
    }

    Optional<A> getAggregate() {
        return Optional.fromNullable(aggregate);
    }

    /**
     * Dispatches the command.
     */
    private A receive(CommandEnvelope envelope) {
        final Action<I, A> action = new Action<>(this, envelope);
        final A result = action.loadAndDispatch();
        return result;
    }

    /**
     * The method object class for dispatching a command to an aggregate.
     *
     * @param <I> the type of aggregate IDs
     * @param <A> the type of the aggregate
     */
    private static class Action<I, A extends Aggregate<I, ?, ?>> {

        private final AggregateRepository<I, A> repository;
        private final CommandEnvelope envelope;
        private final I aggregateId;

        private Action(AggregateCommandEndpoint<I, A> commandEndpoint, CommandEnvelope envelope) {
            this.repository = commandEndpoint.repository;
            this.envelope = envelope;
            this.aggregateId = commandEndpoint.getAggregateId(envelope.getMessage(),
                                                              envelope.getCommandContext());
        }

        /**
         * Loads an aggregate and dispatches the command to it.
         */
        private A loadAndDispatch() {
            final A aggregate = repository.loadOrCreate(aggregateId);

            final LifecycleFlags statusBefore = aggregate.getLifecycleFlags();

            final List<? extends Message> eventMessages = aggregate.dispatchCommand(envelope);

            final AggregateTransaction tx = AggregateTransaction.start(aggregate);
            aggregate.apply(eventMessages, envelope);
            tx.commit();

            // Update status only if the command was handled successfully.
            final LifecycleFlags statusAfter = aggregate.getLifecycleFlags();
            if (statusAfter != null && !statusBefore.equals(statusAfter)) {
                storage().writeLifecycleFlags(aggregateId, statusAfter);
            }

            return aggregate;
        }

        /**
         * Obtains the aggregate storage from the repository.
         */
        private AggregateStorage<I> storage() {
            return repository.aggregateStorage();
        }
    }

    private I getAggregateId(Message commandMessage, CommandContext commandContext) {
        return repository.getAggregateId(commandMessage, commandContext);
    }
}
