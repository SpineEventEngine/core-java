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
public final class ShardConsumerId<E extends MessageEnvelope<?, ?, ?>> {

    private final EntityClass<?> modelClass;
    private final Class<E> observedMsgType;

    private ShardConsumerId(EntityClass<?> modelClass, Class<E> observedMsgType) {
        this.modelClass = modelClass;
        this.observedMsgType = observedMsgType;
    }

    public EntityClass<?> getModelClass() {
        return modelClass;
    }

    public Class<E> getObservedMsgType() {
        return observedMsgType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShardConsumerId that = (ShardConsumerId) o;
        return Objects.equals(modelClass, that.modelClass) &&
                Objects.equals(observedMsgType, that.observedMsgType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelClass, observedMsgType);
    }

    public static ShardConsumerId<CommandEnvelope> forCommandsOf(EntityClass<?> modelClass) {
        return forEnvelope(modelClass, CommandEnvelope.class);
    }

    public static ShardConsumerId<EventEnvelope> forEventsOf(EntityClass<?> modelClass) {
        return forEnvelope(modelClass, EventEnvelope.class);
    }

    public static ShardConsumerId<RejectionEnvelope> forRejectionsOf(EntityClass<?> modelClass) {
        return forEnvelope(modelClass, RejectionEnvelope.class);
    }

    private static <E extends MessageEnvelope<?, ?, ?>> ShardConsumerId<E>
    forEnvelope(EntityClass<?> entityClass, Class<E> envelopeClass) {
        checkNotNull(entityClass);

        final ShardConsumerId<E> id = new ShardConsumerId<>(entityClass, envelopeClass);
        return id;
    }
}
