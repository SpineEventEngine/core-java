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
package io.spine.server.sharding;

import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.entity.EntityClass;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
public final class DeliveryTag<E extends MessageEnvelope<?, ?, ?>> {

    private final BoundedContextName boundedContextName;
    private final EntityClass<?> entityClass;
    private final Class<E> envelopeType;

    private DeliveryTag(BoundedContextName boundedContextName,
                        EntityClass<?> entityClass,
                        Class<E> envelopeType) {
        this.boundedContextName = boundedContextName;
        this.entityClass = entityClass;
        this.envelopeType = envelopeType;
    }

    public EntityClass<?> getEntityClass() {
        return entityClass;
    }

    public Class<E> getEnvelopeType() {
        return envelopeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DeliveryTag<?> that = (DeliveryTag<?>) o;
        return Objects.equals(boundedContextName, that.boundedContextName) &&
                Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(envelopeType, that.envelopeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, entityClass, envelopeType);
    }

    public static DeliveryTag<CommandEnvelope> forCommandsOf(Shardable shardable) {
        checkNotNull(shardable);
        return forEnvelope(shardable.getBoundedContextName(),
                           shardable.getShardedModelClass(),
                           CommandEnvelope.class);
    }

    public static DeliveryTag<EventEnvelope> forEventsOf(Shardable shardable) {
        checkNotNull(shardable);
        return forEnvelope(shardable.getBoundedContextName(),
                           shardable.getShardedModelClass(),
                           EventEnvelope.class);
    }

    public static DeliveryTag<RejectionEnvelope> forRejectionsOf(Shardable shardable) {
        checkNotNull(shardable);
        return forEnvelope(shardable.getBoundedContextName(),
                           shardable.getShardedModelClass(),
                           RejectionEnvelope.class);
    }

    private static <E extends MessageEnvelope<?, ?, ?>> DeliveryTag<E>
    forEnvelope(BoundedContextName bcName, EntityClass<?> entityClass, Class<E> envelopeClass) {
        checkNotNull(bcName);
        checkNotNull(entityClass);
        checkNotNull(envelopeClass);

        final DeliveryTag<E> id = new DeliveryTag<>(bcName, entityClass, envelopeClass);
        return id;
    }
}
