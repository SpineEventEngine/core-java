/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.entity;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.Identifiers;
import org.spine3.base.MessageEnvelope;
import org.spine3.base.Version;

import javax.annotation.Nullable;

/**
 * An envelope holder of the {@linkplain Entity entity} state.
 *
 * @author Alex Tymchenko
 */
public final class EntityStateEnvelope<I, S extends Message>
                                    implements MessageEnvelope<Entity<I, S>> {

    /**
     * The state of the entity.
     */
    private final S entityState;

    /**
     * The ID of the entity, packed as {@code Any}.
     */
    private final Any entityId;

    /**
     * The class of the entity state.
     */
    private final EntityStateClass entityStateClass;

    /**
     * The optional version of an entity.
     *
     * <p>The value is only present If the entity used for the envelope construction
     * is a {@link VersionableEntity}. Otherwise, this field is {@code null}.
     */
    @Nullable
    private final Version entityVersion;

    private EntityStateEnvelope(Entity<I, S> entity) {
        this.entityState = entity.getState();
        this.entityId = Identifiers.idToAny(entity.getId());
        this.entityStateClass = EntityStateClass.of(entity);
        this.entityVersion = entity instanceof VersionableEntity
                             ? ((VersionableEntity) entity).getVersion()
                             : null;
    }

    public static  <I, S extends Message> EntityStateEnvelope of(Entity<I, S> entity) {
        return new EntityStateEnvelope<>(entity);
    }

    /**
     * Always returns {@code null}, as it is impossible to create a {@code Entity} instance basing
     * just on its properties.
     *
     * <p>To obtain an entity instance, use the corresponding {@code Repository} instance along
     * with {@linkplain #getEntityId() entity ID} instead.
     *
     * @return {@code null}
     */
    @SuppressWarnings("ReturnOfNull")
    @Override
    @Nullable
    public Entity<I, S> getOuterObject() {
        return null;
    }

    @Override
    public S getMessage() {
        return entityState;
    }

    @Override
    public EntityStateClass getMessageClass() {
        return this.entityStateClass;
    }

    public I getEntityId() {
        final Object rawId = Identifiers.idFromAny(entityId);
        @SuppressWarnings("unchecked")  // as `Any` was created out of `I`-typed object previously.
        final I result = (I) rawId;
        return result;
    }

    public Optional<Version> getEntityVersion() {
        return Optional.fromNullable(entityVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityStateEnvelope)) {
            return false;
        }
        EntityStateEnvelope<?, ?> that = (EntityStateEnvelope<?, ?>) o;
        return Objects.equal(entityState, that.entityState) &&
               Objects.equal(entityId, that.entityId) &&
               Objects.equal(entityVersion, that.entityVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entityState, entityId, entityVersion);
    }
}
