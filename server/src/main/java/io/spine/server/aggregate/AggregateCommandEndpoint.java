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

package io.spine.server.aggregate;

import io.spine.core.Event;
import io.spine.server.command.DispatchCommand;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import java.util.List;

import static io.spine.server.command.DispatchCommand.operationFor;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Dispatches commands to aggregates of the associated {@code AggregateRepository}.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by the parent repository
 */
final class AggregateCommandEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEndpoint<I, A, CommandEnvelope> {

    AggregateCommandEndpoint(AggregateRepository<I, A> repo, CommandEnvelope command) {
        super(repo, command);
    }

    @Override
    protected List<Event> invokeDispatcher(A aggregate, CommandEnvelope envelope) {
        EntityLifecycle lifecycle = repository().lifecycleOf(aggregate.getId());
        DispatchCommand<I> dispatch = operationFor(lifecycle, aggregate, envelope);
        return dispatch.perform();
    }

    @Override
    protected void onError(CommandEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }

    /**
     * Throws {@link IllegalStateException} with the message containing details of the aggregate and
     * the command in response to which the aggregate generated empty set of event messages.
     *
     * @throws IllegalStateException always
     */
    @Override
    protected void onEmptyResult(A aggregate, CommandEnvelope cmd) throws IllegalStateException {
        String format = "The aggregate (class: %s, id: %s) produced empty response for " +
                        "the command (class: %s, id: %s).";
        onUnhandledCommand(aggregate, cmd, format);
    }

    /**
     * Throws {@link IllegalStateException} with the diagnostics message on the unhandled command.
     *
     * @param aggregate
     *         the aggregate which failed to handle the command
     * @param cmd
     *         the envelope with the command
     * @param format
     *         the format string with the parameters as follows:
     *          <ol>
     *              <li>the name of the aggregate class;
     *              <li>the ID of the aggregate;
     *              <li>the name of the command class;
     *              <li>the ID of the command.
     *          </ol>
     * @throws IllegalStateException
     *         always
     */
    private void onUnhandledCommand(A aggregate, CommandEnvelope cmd, String format) {
        String entityId = aggregate.idAsString();
        String entityClass = aggregate.getClass()
                                      .getName();
        String commandId = cmd.idAsString();
        CommandClass commandClass = cmd.getMessageClass();
        throw newIllegalStateException(format, entityClass, entityId, commandClass, commandId);
    }
}
