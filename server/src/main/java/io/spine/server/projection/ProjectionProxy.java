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
import io.spine.server.delivery.Delivery;
import io.spine.server.entity.EntityLifecycleMonitor;
import io.spine.server.entity.EntityProxy;
import io.spine.server.entity.Repository;
import io.spine.server.entity.TransactionListener;

import java.util.List;

/**
 * Dispatches an event to projections.
 */
@Internal
public class ProjectionProxy<I, P extends Projection<I, ?, ?>>
        extends EntityProxy<I, P, EventEnvelope> {

    protected ProjectionProxy(Repository<I, P> repository, I projectionId) {
        super(repository, projectionId);
    }

    @Override
    protected ProjectionRepository<I, P, ?> repository() {
        return (ProjectionRepository<I, P, ?>) super.repository();
    }

    @Override
    protected void deliverNow(EventEnvelope envelope) {
        ProjectionRepository<I, P, ?> repository = repository();
        P projection = repository.findOrCreate(entityId());
        dispatchInTx(projection, envelope);
        store(projection, envelope);
    }

    protected void dispatchInTx(P projection, EventEnvelope envelope) {
        ProjectionTransaction<I, ?, ?> tx =
                ProjectionTransaction.start((Projection<I, ?, ?>) projection);
        TransactionListener listener = EntityLifecycleMonitor.newInstance(repository());
        tx.setListener(listener);
        doDispatch(projection, envelope);
        tx.commit();
    }

    @Override
    protected Delivery<I, P, EventEnvelope, ?, ?> getDelivery() {
        return repository().getDelivery();
    }

    @CanIgnoreReturnValue
    @Override
    protected List<Event> doDispatch(P projection, EventEnvelope event) {
        projection.play(event.getOuterObject());
        return ImmutableList.of();
    }

    @Override
    protected boolean isModified(P projection) {
        boolean result = projection.isChanged();
        return result;
    }

    @Override
    protected void onModified(P projection, EventEnvelope envelope) {
        ProjectionRepository<I, P, ?> repository = repository();
        repository.store(projection);

        EventContext eventContext = envelope.getEventContext();
        Timestamp eventTime = eventContext.getTimestamp();
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
