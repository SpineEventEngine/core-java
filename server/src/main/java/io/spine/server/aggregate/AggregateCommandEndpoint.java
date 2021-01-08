/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import io.spine.server.command.DispatchCommand;
import io.spine.server.delivery.CommandEndpoint;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;

import static io.spine.server.command.DispatchCommand.operationFor;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Dispatches commands to aggregates of the associated {@code AggregateRepository}.
 *
 * @param <I> the type of the aggregate IDs
 * @param <A> the type of the aggregates managed by the parent repository
 */
final class AggregateCommandEndpoint<I, A extends Aggregate<I, ?, ?>>
        extends AggregateEndpoint<I, A, CommandEnvelope>
        implements CommandEndpoint<I> {

    AggregateCommandEndpoint(AggregateRepository<I, A> repo, CommandEnvelope command) {
        super(repo, command);
    }

    @Override
    protected DispatchOutcome invokeDispatcher(A aggregate) {
        EntityLifecycle lifecycle = repository().lifecycleOf(aggregate.id());
        DispatchCommand<I> dispatch = operationFor(lifecycle, aggregate, envelope());
        return dispatch.perform();
    }

    @Override
    protected void afterDispatched(I entityId) {
        repository().lifecycleOf(entityId)
                    .onDispatchCommand(envelope().command());
    }

    /**
     * Throws {@link IllegalStateException} with the message containing details of the aggregate and
     * the command in response to which the aggregate generated empty set of event messages.
     *
     * @throws IllegalStateException always
     */
    @Override
    protected void onEmptyResult(A aggregate) throws IllegalStateException {
        CommandEnvelope cmd = envelope();
        String entityId = aggregate.idAsString();
        String entityClass = aggregate.getClass()
                                      .getName();
        String commandId = cmd.id().value();
        CommandClass commandClass = cmd.messageClass();
        throw newIllegalStateException(
                "The aggregate (class: %s, ID: %s) produced empty response for " +
                        "the command (class: %s, ID: %s).",
                entityClass, entityId, commandClass, commandId
        );
    }
}
