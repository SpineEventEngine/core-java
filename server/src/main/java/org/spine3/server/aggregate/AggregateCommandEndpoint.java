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

package org.spine3.server.aggregate;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Stringifiers;
import org.spine3.server.entity.status.EntityStatus;
import org.spine3.server.storage.TenantDataOperation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;

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
        extends TenantDataOperation {

    private final AggregateRepository<I, A> repository;
    private final Command command;

    static <I, A extends Aggregate<I, ?, ?>>
            AggregateCommandEndpoint<I, A> createFor(AggregateRepository<I, A> repository,
                                                     Command command) {
        return new AggregateCommandEndpoint<>(repository, command);
    }

    private AggregateCommandEndpoint(AggregateRepository<I, A> repository,
                                     Command command) {
        super(command.getContext().getTenantId());
        this.repository = repository;
        this.command = command;
    }

    @Override
    public void run() {
        final A aggregate = dispatch(command);
        repository.afterDispatch(aggregate);
    }

    /**
     * Dispatches the command.
     *
     * @return the aggregate to which the command was dispatched
     */
    A dispatch(Command command) {
        final Action<I, A> action = new Action<>(this, command);
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
        private final Message commandMessage;
        private final CommandContext context;
        private final I aggregateId;

        private Action(AggregateCommandEndpoint<I, A> commandEndpoint, Command command) {
            this.repository = commandEndpoint.repository;

            this.commandMessage = getMessage(checkNotNull(command));
            this.context = command.getContext();
            this.aggregateId = commandEndpoint.getAggregateId(commandMessage, context);
        }

        /**
         * Loads an aggregate and dispatches the command to it.
         *
         * <p>During the command dispatching and event applying, the original list of events may
         * have been changed by other actors in the system.
         *
         * <p>To ensure the resulting {@code Aggregate} state is consistent with the numerous
         * concurrent actor changes, the event count from the last snapshot should remain the same
         * during the {@link AggregateRepository#load(Object)}
         * and {@link Aggregate#dispatchCommand(Message, CommandContext)}.
         *
         * <p>In case the new events are detected, {@code Aggregate} loading and {@code Command}
         * dispatching is repeated from scratch.
         */
        private A loadAndDispatch() {
            final AggregateStorage<I> storage = storage();
            A aggregate;
            Integer eventCountBeforeSave = null;
            int eventCountBeforeDispatch = 0;
            do {
                if (eventCountBeforeSave != null) {
                    final int newEventCount = eventCountBeforeSave - eventCountBeforeDispatch;
                    logConcurrentModification(aggregateId, commandMessage, newEventCount);
                }

                eventCountBeforeDispatch = storage.readEventCountAfterLastSnapshot(aggregateId);

                aggregate = doDispatch();

                eventCountBeforeSave = storage.readEventCountAfterLastSnapshot(aggregateId);
            } while (eventCountBeforeDispatch != eventCountBeforeSave);

            return aggregate;
        }

        private A doDispatch() {
            final A aggregate = repository.loadOrCreate(aggregateId);

            final EntityStatus statusBefore = aggregate.getStatus();

            aggregate.dispatchCommand(commandMessage, context);

            // Update status only if the command was handled successfully.
            final EntityStatus statusAfter = aggregate.getStatus();
            if (statusAfter != null && !statusBefore.equals(statusAfter)) {
                storage().writeStatus(aggregateId, statusAfter);
            }

            return aggregate;
        }

        /**
         * Obtains the aggregate storage from the repository.
         */
        private AggregateStorage<I> storage() {
            return repository.aggregateStorage();
        }

        private void logConcurrentModification(I aggregateId, Message commandMessage, int newEventCount) {
            final String idStr = Stringifiers.idToString(aggregateId);
            final Class<? extends Aggregate<I, ?, ?>> aggregateClass = repository.getAggregateClass();
            AggregateRepository.log()
                               .warn("Detected the concurrent modification of {} ID: {}. " +
                                             "New events detected while dispatching the command {} " +
                                             "The number of new events is {}. " +
                                             "Restarting the command dispatching.",
                                     aggregateClass, idStr, commandMessage, newEventCount);
        }
    }

    private I getAggregateId(Message commandMessage, CommandContext commandContext) {
        return repository.getAggregateId(commandMessage, commandContext);
    }
}
