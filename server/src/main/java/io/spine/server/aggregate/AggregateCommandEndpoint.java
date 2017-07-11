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
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.tenant.CommandOperation;
import io.spine.server.tenant.TenantAwareOperation;

import javax.annotation.Nullable;
import java.util.List;

import static io.spine.util.Exceptions.newIllegalStateException;

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

    private AggregateCommandEndpoint(AggregateRepository<I, A> repo, CommandEnvelope envelope) {
        super(envelope.getTenantId());
        this.repository = repo;
        this.command = envelope;
    }

    static <I, A extends Aggregate<I, ?, ?>>
    void handle(final AggregateRepository<I, A> repository, final CommandEnvelope envelope) {
        final AggregateCommandEndpoint<I, A> commandEndpoint =
                new AggregateCommandEndpoint<>(repository, envelope);

        final Command command = envelope.getCommand();
        final CommandOperation op = new CommandOperation(command) {
            @Override
            public void run() {
                commandEndpoint.execute();

                final Optional<A> processedAggregate = commandEndpoint.processedAggregate();
                if (!processedAggregate.isPresent()) {
                    throw newIllegalStateException(
                            "No aggregate loaded for command (class: %s, id: %s)",
                            envelope.getMessageClass(),
                            envelope.getId());
                }

                final A aggregate = processedAggregate.get();
                final List<Event> events = aggregate.getUncommittedEvents();

                repository.store(aggregate);
                repository.updateStand(aggregate, command.getContext());
                repository.postEvents(events);
            }
        };
        op.execute();
    }

    @Override
    public void run() {
        aggregate = receive(command);
    }

    Optional<A> processedAggregate() {
        return Optional.fromNullable(aggregate);
    }

    /**
     * Dispatches the command.
     */
    private A receive(CommandEnvelope envelope) {
        final I aggregateId = getAggregateId(envelope);
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

    private AggregateStorage<I> storage() {
        return repository.aggregateStorage();
    }

    private I getAggregateId(CommandEnvelope envelope) {
        return repository.getAggregateId(envelope.getMessage(), envelope.getCommandContext());
    }
}
