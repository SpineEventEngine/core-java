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
package io.spine.server.delivery;

import com.google.common.base.MoreObjects;
import io.spine.core.BoundedContextName;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.entity.EntityClass;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value, that defines the message destination in the {@linkplain Delivery delivery process}.
 *
 * <p>In the message delivery process, envelopes are travelling from some {@linkplain Sender sender}
 * through the {@linkplain ShardedStream sharded message stream} to a number of {@linkplain Consumer
 * consumers}. To identify which consumers, should receive the message, an instance of
 * {@code DeliveryTag} is used.
 *
 * <p>The value of the tag identifies the entity, which exists in scope of some bounded context and
 * declares the need to consume message envelopes of a specific type (such as
 * {@link CommandEnvelope}).
 *
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

    public BoundedContextName getBoundedContextName() {
        return boundedContextName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryTag<?> that = (DeliveryTag<?>) o;
        return Objects.equals(boundedContextName, that.boundedContextName) &&
                Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(envelopeType, that.envelopeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, entityClass, envelopeType);
    }

    /**
     * Creates an instance of {@code DeliveryTag} that identifies the need to deliver command
     * envelopes to entities that are sharded within the given {@code Shardable}.
     *
     * @param shardable the shardable, which entities declare the need in command envelopes
     * @return the new instance of {@code DeliveryTag}
     */
    public static DeliveryTag<CommandEnvelope> forCommandsOf(Shardable shardable) {
        checkNotNull(shardable);
        return forEnvelope(shardable.getBoundedContextName(),
                           shardable.getShardedModelClass(),
                           CommandEnvelope.class);
    }

    /**
     * Creates an instance of {@code DeliveryTag} that identifies the need to deliver event
     * envelopes to entities that are sharded within the given {@code Shardable}.
     *
     * @param shardable the shardable, which entities declare the need in event envelopes
     * @return the new instance of {@code DeliveryTag}
     */
    public static DeliveryTag<EventEnvelope> forEventsOf(Shardable shardable) {
        checkNotNull(shardable);
        return forEnvelope(shardable.getBoundedContextName(),
                           shardable.getShardedModelClass(),
                           EventEnvelope.class);
    }

    /**
     * Creates an instance of {@code DeliveryTag} that identifies the need to deliver rejection
     * envelopes to entities that are sharded within the given {@code Shardable}.
     *
     * @param shardable the shardable, which entities declare the need in command rejection
     * @return the new instance of {@code DeliveryTag}
     */
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

        DeliveryTag<E> id = new DeliveryTag<>(bcName, entityClass, envelopeClass);
        return id;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("BoundedContext name", boundedContextName)
                          .add("entityClass", entityClass)
                          .add("envelopeType", envelopeType)
                          .toString();
    }
}
