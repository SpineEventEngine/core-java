/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.Repository;
import io.spine.server.entity.TransactionListener;

import java.util.List;

import static io.spine.server.projection.ProjectionTransaction.start;

/**
 * Dispatches an event to projections.
 */
@Internal
public class ProjectionEndpoint<I, P extends Projection<I, ?, ?>>
        extends EntityMessageEndpoint<I, P, EventEnvelope> {

    protected ProjectionEndpoint(Repository<I, P> repository, EventEnvelope event) {
        super(repository, event);
    }

    static <I, P extends Projection<I, ?, ?>>
    ProjectionEndpoint<I, P> of(ProjectionRepository<I, P, ?> repository, EventEnvelope event) {
        return new ProjectionEndpoint<>(repository, event);
    }

    @Override
    protected ProjectionRepository<I, P, ?> repository() {
        return (ProjectionRepository<I, P, ?>) super.repository();
    }

    @Override
    protected void dispatchInTx(I entityId) {
        ProjectionRepository<I, P, ?> repository = repository();
        P projection = repository.findOrCreate(entityId);
        runTransactionFor(projection);
        store(projection);
    }

    @SuppressWarnings("unchecked") // Simplify massive generic args.
    protected void runTransactionFor(P projection) {
        ProjectionTransaction<I, ?, ?> tx = start((Projection<I, ?, ?>) projection);
        TransactionListener listener = EntityLifecycleMonitor.newInstance(repository());
        tx.setListener(listener);
        invokeDispatcher(projection, envelope());
        tx.commit();
    }

    @CanIgnoreReturnValue
    @Override
    protected List<Event> invokeDispatcher(P projection, EventEnvelope event) {
        projection.play(event.getOuterObject());
        return ImmutableList.of();
    }

    @Override
    protected boolean isModified(P projection) {
        boolean result = projection.isChanged();
        return result;
    }

    @Override
    protected void onModified(P projection) {
        ProjectionRepository<I, P, ?> repository = repository();
        repository.store(projection);

        EventContext eventContext = envelope().getEventContext();
        Timestamp eventTime = eventContext.getTimestamp();
        repository.projectionStorage()
                  .writeLastHandledEventTime(eventTime);

        repository.getStand()
                  .post(envelope().getTenantId(), projection);
    }

    /**
     * Does nothing since a state of a projection should not be necessarily
     * updated upon execution of a {@linkplain io.spine.core.Subscribe subscriber} method.
     */
    @Override
    protected void onEmptyResult(P entity, EventEnvelope event) {
        // Do nothing.
    }

    @Override
    protected void onError(EventEnvelope event, RuntimeException exception) {
        repository().onError(event, exception);
    }
}
