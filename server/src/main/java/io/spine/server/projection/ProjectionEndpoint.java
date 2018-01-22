/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.server.delivery.EndpointDelivery;
import io.spine.server.entity.EntityMessageEndpoint;
import io.spine.server.entity.Repository;

import java.util.List;
import java.util.Set;

/**
 * Dispatches an event to projections.
 */
class ProjectionEndpoint<I, P extends Projection<I, ?, ?>>
        extends EntityMessageEndpoint<I, P, EventEnvelope, Set<I>> {

    private ProjectionEndpoint(Repository<I, P> repository, EventEnvelope event) {
        super(repository, event);
    }

    static <I, P extends Projection<I, ?, ?>>
    ProjectionEndpoint<I, P> of(ProjectionRepository<I, P, ?> repository, EventEnvelope event) {
        return new ProjectionEndpoint<>(repository, event);
    }

    static <I, P extends Projection<I, ?, ?>>
    Set<I> handle(ProjectionRepository<I, P, ?> repository, EventEnvelope event) {
        final ProjectionEndpoint<I, P> endpoint = of(repository, event);
        final Set<I> result = endpoint.handle();
        return result;
    }

    @Override
    protected ProjectionRepository<I, P, ?> repository() {
        return (ProjectionRepository<I, P, ?>) super.repository();
    }

    @Override
    protected Set<I> getTargets() {
        final EventEnvelope event = envelope();
        final Set<I> ids = repository().eventRouting()
                                       .apply(event.getMessage(), event.getEventContext());
        return ids;
    }

    @Override
    protected void deliverNowTo(I entityId) {
        final P projection = repository().findOrCreate(entityId);
        final ProjectionTransaction<I, ?, ?> tx =
                ProjectionTransaction.start((Projection<I, ?, ?>) projection);
        projection.handle(envelope());
        tx.commit();
        store(projection);
    }

    @Override
    protected EndpointDelivery<I, P, EventEnvelope> getEndpointDelivery(EventEnvelope event) {
        return repository().getEndpointDelivery();
    }

    @Override
    protected List<? extends Message> doDispatch(P projection, EventEnvelope event) {
        projection.handle(event);
        return ImmutableList.of();
    }

    @Override
    protected boolean isModified(P projection) {
        final boolean result = projection.isChanged();
        return result;
    }

    @Override
    protected void onModified(P projection) {
        final ProjectionRepository<I, P, ?> repository = repository();
        repository.store(projection);

        final EventContext eventContext = envelope().getEventContext();
        final Timestamp eventTime = eventContext.getTimestamp();
        repository.projectionStorage()
                  .writeLastHandledEventTime(eventTime);

        repository.getStand()
                  .post(eventContext.getCommandContext()
                                    .getActorContext()
                                    .getTenantId(), projection);
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
