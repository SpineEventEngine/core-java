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
public final class ShardingTag<E extends MessageEnvelope<?, ?, ?>> {

    private final EntityClass<?> entityClass;
    private final Class<E> envelopeType;

    private ShardingTag(EntityClass<?> entityClass, Class<E> envelopeType) {
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
        ShardingTag that = (ShardingTag) o;
        return Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(envelopeType, that.envelopeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, envelopeType);
    }

    public static ShardingTag<CommandEnvelope> forCommandsOf(EntityClass<?> modelClass) {
        return forEnvelope(modelClass, CommandEnvelope.class);
    }

    public static ShardingTag<EventEnvelope> forEventsOf(EntityClass<?> modelClass) {
        return forEnvelope(modelClass, EventEnvelope.class);
    }

    public static ShardingTag<RejectionEnvelope> forRejectionsOf(EntityClass<?> modelClass) {
        return forEnvelope(modelClass, RejectionEnvelope.class);
    }

    private static <E extends MessageEnvelope<?, ?, ?>> ShardingTag<E>
    forEnvelope(EntityClass<?> entityClass, Class<E> envelopeClass) {
        checkNotNull(entityClass);

        final ShardingTag<E> id = new ShardingTag<>(entityClass, envelopeClass);
        return id;
    }
}
