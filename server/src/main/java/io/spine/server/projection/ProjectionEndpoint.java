/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.projection;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.base.Error;
import io.spine.server.delivery.EventEndpoint;
import io.spine.server.dispatch.DispatchOutcome;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.entity.TransactionListener;
import io.spine.server.type.EventEnvelope;

import static io.spine.server.projection.ProjectionTransaction.start;

/**
 * Dispatches an event to projections.
 */
@Internal
public class ProjectionEndpoint<I, P extends Projection<I, S, ?>, S extends EntityState>
        extends EntityMessageEndpoint<I, P, EventEnvelope>
        implements EventEndpoint<I> {

    protected ProjectionEndpoint(Repository<I, P> repository, EventEnvelope event) {
        super(repository, event);
    }

    static <I, P extends Projection<I, S, ?>, S extends EntityState>
    ProjectionEndpoint<I, P, S> of(ProjectionRepository<I, P, ?> repository, EventEnvelope event) {
        return new ProjectionEndpoint<>(repository, event);
    }

    @Override
    public ProjectionRepository<I, P, ?> repository() {
        return (ProjectionRepository<I, P, ?>) super.repository();
    }

    @Override
    protected DispatchOutcome performDispatch(I entityId) {
        ProjectionRepository<I, P, ?> repository = repository();
        P projection = repository.findOrCreate(entityId);
        DispatchOutcome outcome = runTransactionFor(projection);
        store(projection);
        return outcome;
    }

    @Override
    protected void afterDispatched(I entityId) {
        repository().lifecycleOf(entityId)
                    .onDispatchEventToSubscriber(envelope().outerObject());
    }

    protected DispatchOutcome runTransactionFor(P projection) {
        ProjectionTransaction<I, S, ?> tx = start((Projection<I, S, ?>) projection);
        TransactionListener<I> listener =
                EntityLifecycleMonitor.newInstance(repository(), projection.id());
        tx.setListener(listener);
        DispatchOutcome outcome = invokeDispatcher(projection);
        tx.commitIfActive();
        if (outcome.hasSuccess()) {
            afterDispatched(projection.id());
        } else if (outcome.hasError()) {
            Error error = outcome.getError();
            repository().lifecycleOf(projection.id())
                        .onDispatchingFailed(envelope(), error);
        }
        return outcome;
    }

    @CanIgnoreReturnValue
    @Override
    protected DispatchOutcome invokeDispatcher(P projection) {
        return projection.play(envelope().outerObject());
    }

    @Override
    protected boolean isModified(P projection) {
        boolean result = projection.changed();
        return result;
    }

    @Override
    protected void onModified(P projection) {
        ProjectionRepository<I, P, ?> repository = repository();
        repository.store(projection);
    }

    /**
     * Does nothing since a state of a projection should not be necessarily
     * updated upon execution of a {@linkplain io.spine.core.Subscribe subscriber} method.
     */
    @Override
    protected void onEmptyResult(P entity) {
        // Do nothing.
    }
}
