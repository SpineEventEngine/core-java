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

import io.spine.annotation.SPI;
import io.spine.core.EventEnvelope;
import io.spine.server.delivery.Consumer;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.DeliveryTag;
import io.spine.server.delivery.EventShardedStream;
import io.spine.type.MessageClass;

/**
 * A strategy on delivering the events to the instances of a certain projection type.
 *
 * <p>Spine users may want to extend this class in order to switch to another delivery mechanism.
 *
 * @param <I> the ID type of projection, to which events are being delivered
 * @param <P> the type of projection
 * @author Alexander Yevsyukov
 */
@SPI
public class ProjectionEventDelivery<I, P extends Projection<I, ?, ?>>
        extends Delivery<I,
                         P,
                         EventEnvelope,
                         EventShardedStream<I>,
                         EventShardedStream.Builder<I>> {

    protected ProjectionEventDelivery(ProjectionRepository<I, P, ?> repository) {
        super(new ProjectionEventConsumer<>(repository));
    }

    private static class ProjectionEventConsumer<I, P extends Projection<I, ?, ?>>
            extends Consumer<I,
                             P,
                             EventEnvelope,
                             EventShardedStream<I>,
                             EventShardedStream.Builder<I>> {

        protected ProjectionEventConsumer(ProjectionRepository<I, P, ?> repository) {
            super(DeliveryTag.forEventsOf(repository), repository);
        }

        @Override
        protected EventShardedStream.Builder<I> newShardedStreamBuilder() {
            return EventShardedStream.newBuilder();
        }

        @Override
        protected ProjectionRepository<I, P, ?> repository() {
            return (ProjectionRepository<I, P, ?>) super.repository();
        }

        @Override
        protected ProjectionEndpoint<I, P> proxyFor(I entityId, MessageClass targetClass) {
            return new ProjectionEndpoint<>(repository(), entityId);
        }

        @Override
        protected void passToEndpoint(I id, EventEnvelope message) {
            proxyFor(id, message.getMessageClass()).deliverNow(message);
        }
    }
}
