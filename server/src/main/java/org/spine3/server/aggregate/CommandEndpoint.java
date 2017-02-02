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
import org.spine3.base.CommandId;
import org.spine3.base.Errors;
import org.spine3.base.FailureThrowable;
import org.spine3.base.Stringifiers;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandStatusService;
import org.spine3.server.entity.idfunc.GetTargetIdFromCommand;
import org.spine3.server.entity.status.EntityStatus;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Commands.getMessage;

/**
 * Dispatches commands to aggregates of the associated {@code AggregateRepository}.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by this repository
 * @author Alexander Yevsyukov
 */
class CommandEndpoint<I, A extends Aggregate<I, ?, ?>> {

    private final GetTargetIdFromCommand<I, Message> getIdFunction = GetTargetIdFromCommand.newInstance();

    private final AggregateRepository<I, A> aggregateRepository;
    private final CommandStatusService commandStatusService;

    CommandEndpoint(AggregateRepository<I, A> repository) {
        this.aggregateRepository = repository;
        final BoundedContext boundedContext = repository.getBoundedContext();
        this.commandStatusService = boundedContext.getCommandBus()
                                                  .getCommandStatusService();
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

        private final CommandEndpoint<I, A> commandEndpoint;
        private final AggregateRepository<I, A> aggregateRepository;
        private final CommandId commandId;
        private final Message commandMessage;
        private final CommandContext context;
        private final I aggregateId;

        private Action(CommandEndpoint<I, A> commandEndpoint, Command command) {
            this.commandEndpoint = commandEndpoint;
            this.aggregateRepository = commandEndpoint.aggregateRepository;

            this.commandMessage = getMessage(checkNotNull(command));
            this.context = command.getContext();
            this.commandId = context.getCommandId();
            this.aggregateId = commandEndpoint.getAggregateId(commandMessage);
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
         * and {@link Aggregate#dispatch(Message, CommandContext)}.
         *
         * <p>In case the new events are detected, {@code Aggregate} loading and {@code Command}
         * dispatching is repeated from scratch.
         */
        private A loadAndDispatch() {
            A aggregate;

            Integer eventCountBeforeSave = null;
            int eventCountBeforeDispatch = 0;
            do {
                if (eventCountBeforeSave != null) {
                    final int newEventCount = eventCountBeforeSave - eventCountBeforeDispatch;
                    logConcurrentModification(aggregateId, commandMessage, newEventCount);
                }

                eventCountBeforeDispatch = getAggregateStorage().readEventCountAfterLastSnapshot(aggregateId);

                aggregate = doDispatch();

                eventCountBeforeSave = getAggregateStorage().readEventCountAfterLastSnapshot(aggregateId);
            } while (eventCountBeforeDispatch != eventCountBeforeSave);

            return aggregate;
        }

        private A doDispatch() {
            A aggregate = aggregateRepository.loadOrCreate(aggregateId);

            final EntityStatus statusBefore = aggregate.getStatus();
            EntityStatus statusAfter = null;
            try {
                aggregate.dispatch(commandMessage, context);
                statusAfter = aggregate.getStatus();
            } catch (RuntimeException e) {
                commandEndpoint.updateCommandStatus(commandId, e);
            } finally {
                if (statusAfter != null && !statusBefore.equals(statusAfter)) {
                    getAggregateStorage().writeStatus(aggregateId, statusAfter);
                }
            }
            return aggregate;
        }

        private void logConcurrentModification(I aggregateId, Message commandMessage, int newEventCount) {
            final String idStr = Stringifiers.idToString(aggregateId);
            final Class<? extends Aggregate<I, ?, ?>> aggregateClass = aggregateRepository.getAggregateClass();
            AggregateRepository.log()
                               .warn("Detected the concurrent modification of {} ID: {}. " +
                                             "New events detected while dispatching the command {} " +
                                             "The number of new events is {}. " +
                                             "Restarting the command dispatching.",
                                     aggregateClass, idStr, commandMessage, newEventCount);
        }

        private AggregateStorage<I> getAggregateStorage() {
            return aggregateRepository.aggregateStorage();
        }
    }

    private I getAggregateId(Message command) {
        final I id = getIdFunction.apply(command, CommandContext.getDefaultInstance());
        return id;
    }

    @SuppressWarnings("ChainOfInstanceofChecks") // OK for this rare case of handing an exception.
    private void updateCommandStatus(CommandId commandId, RuntimeException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof Exception) {
            final Exception exception = (Exception) cause;
            commandStatusService.setToError(commandId, exception);
        } else if (cause instanceof FailureThrowable) {
            final FailureThrowable failure = (FailureThrowable) cause;
            commandStatusService.setToFailure(commandId, failure);
        } else {
            commandStatusService.setToError(commandId, Errors.fromThrowable(cause));
        }
    }
}
