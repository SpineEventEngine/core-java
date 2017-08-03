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

import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.server.tenant.TenantAwareFunction0;
import io.spine.string.Stringifiers;

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
    extends AggregateMessageEndpoint<I, A, CommandEnvelope, I> {

    private AggregateCommandEndpoint(AggregateRepository<I, A> repo, CommandEnvelope envelope) {
        super(repo, envelope);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    I handle(AggregateRepository<I, A> repository, CommandEnvelope envelope) {
        final AggregateCommandEndpoint<I, A> commandEndpoint =
                new AggregateCommandEndpoint<>(repository, envelope);

        final TenantAwareFunction0<I> operation = commandEndpoint.createOperation();
        return operation.execute();
    }

    @Override
    protected List<? extends Message> dispatchEnvelope(A aggregate, CommandEnvelope envelope) {
        return aggregate.dispatchCommand(envelope);
    }

    /**
     * Throws {@link IllegalStateException} with the message containing details of the aggregate and
     * the command in response to which the aggregate generated empty set of event messages.
     * @throws IllegalStateException always
     */
    @Override
    protected void onEmptyResult(A aggregate, CommandEnvelope envelope)
            throws IllegalStateException {
        throw newIllegalStateException(
                "The aggregate (class: %s, id: %s) produced empty response for " +
                        "command (class: %s, id: %s).",
                aggregate.getClass()
                         .getName(),
                Stringifiers.toString(aggregate.getId()),
                envelope.getMessageClass(),
                Stringifiers.toString(envelope.getId()));
    }

    @Override
    protected void onError(CommandEnvelope envelope, RuntimeException exception) {
        repository().onError(envelope, exception);
    }

    @Override
    protected TenantAwareFunction0<I> createOperation() {
        return new Operation(envelope().getCommand());
    }

    /**
     * Returns immutable set containing ID of the aggregate that is responsible for
     * handling the command.
     */
    @Override
    protected I getTargets() {
        final CommandEnvelope envelope = envelope();
        final I commandTarget = repository().getCommandTarget(envelope.getMessage(),
                                                              envelope.getCommandContext());
        return commandTarget;
    }

    /**
     * The operation executed under the command's tenant.
     */
    private class Operation extends TenantAwareFunction0<I> {

        private Operation(Command command) {
            super(command.getContext()
                         .getActorContext()
                         .getTenantId());
        }

        @Override
        public I apply() {
            return dispatch();
        }
    }
}
