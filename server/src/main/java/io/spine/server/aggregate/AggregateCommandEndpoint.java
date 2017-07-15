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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.server.tenant.CommandOperation;
import io.spine.server.tenant.TenantAwareOperation;

import java.util.List;
import java.util.Set;

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
    extends AggregateMessageEndpoint<I, A, CommandEnvelope> {

    private AggregateCommandEndpoint(AggregateRepository<I, A> repo, CommandEnvelope envelope) {
        super(repo, envelope);
    }

    static <I, A extends Aggregate<I, ?, ?>>
    void handle(AggregateRepository<I, A> repository, CommandEnvelope envelope) {
        final AggregateCommandEndpoint<I, A> commandEndpoint =
                new AggregateCommandEndpoint<>(repository, envelope);

        final TenantAwareOperation operation = commandEndpoint.createOperation();
        operation.execute();
    }

    @Override
    protected List<? extends Message> dispatchEnvelope(A aggregate, CommandEnvelope envelope) {
        return aggregate.dispatchCommand(envelope);
    }

    @Override
    protected TenantAwareOperation createOperation() {
        return new Operation(envelope().getCommand());
    }

    /**
     * Returns immutable set containing ID of the aggregate that is responsible for
     * handling the command.
     */
    @Override
    protected Set<I> getTargets() {
        final CommandEnvelope envelope = envelope();
        return ImmutableSet.of(repository().getCommandTarget(envelope.getMessage(),
                                                             envelope.getCommandContext()));
    }

    /**
     * The operation executed under the command's tenant.
     */
    private class Operation extends CommandOperation {

        private Operation(Command command) {
            super(command);
        }

        @Override
        public void run() {
            dispatch();
        }
    }
}
