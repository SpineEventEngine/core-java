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

import io.spine.annotation.SPI;
import io.spine.core.EventEnvelope;
import io.spine.server.delivery.EndpointDelivery;

/**
 * A strategy on delivering the events to the instances of a certain projection type.
 *
 * @param <I> the ID type of projection, to which events are being delivered
 * @param <P> the type of projection
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class ProjectionEventDelivery<I, P extends Projection<I, ?, ?>>
        extends EndpointDelivery<I, P, EventEnvelope> {

    protected ProjectionEventDelivery(ProjectionRepository<I, P, ?> repository) {
        super(repository, repository.projectionClass());
    }

    public static <I, P extends Projection<I, ?, ?>>
    ProjectionEventDelivery<I, P> directDelivery(ProjectionRepository<I, P, ?> repository) {
        return new Direct<>(repository);
    }

    @Override
    protected ProjectionRepository<I, P, ?> repository() {
        return (ProjectionRepository<I, P, ?>) super.repository();
    }

    @Override
    protected ProjectionEndpoint<I, P> getEndpoint(EventEnvelope event) {
        return ProjectionEndpoint.of(repository(), event);
    }

    @Override
    protected void passToEndpoint(I id, EventEnvelope event) {
        getEndpoint(event).deliverNowTo(id);
    }

    /**
     * Direct delivery which does not postpone dispatching.
     *
     * @param <I> the type of projection IDs
     * @param <P> the type of projections
     */
    public static class Direct<I, P extends Projection<I, ?, ?>>
            extends ProjectionEventDelivery<I, P> {

        private Direct(ProjectionRepository<I, P, ?> repository) {
            super(repository);
        }

        @Override
        public boolean shouldPostpone(I id, EventEnvelope envelope) {
            return false;
        }
    }
}
