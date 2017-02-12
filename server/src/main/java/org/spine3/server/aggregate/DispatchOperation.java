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

import org.spine3.base.Command;
import org.spine3.server.storage.TenantDataOperation;

import static org.spine3.server.aggregate.AggregateCommandEndpoint.createFor;

/**
 * Dispatches a command to an aggregate.
 *
 * <p>Loading and storing an aggregate is a tenant-sensitive operation,
 * which depends on the tenant ID of the command we dispatch.
 *
 * @param <I> the type of aggregate IDs
 * @param <A> the type of the aggregate
 * @author Alexander Yevsyukov
 * @see TenantDataOperation
 */
class DispatchOperation<I, A extends Aggregate<I, ?, ?>>
        extends TenantDataOperation {

    private final AggregateRepository<I, A> repository;
    private final Command command;

    protected DispatchOperation(AggregateRepository<I, A> repository, Command command) {
        super(command.getContext().getTenantId());
        this.repository = repository;
        this.command = command;
    }

    @Override
    public void run() {
        final AggregateCommandEndpoint<I, A> commandEndpoint = createFor(repository);
        final A aggregate = commandEndpoint.dispatch(command);
        repository.afterDispatch(aggregate);
    }
}
