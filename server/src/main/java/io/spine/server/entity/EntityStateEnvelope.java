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
package io.spine.server.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.core.TenantId;
import io.spine.core.Version;
import io.spine.server.entity.model.EntityStateClass;
import io.spine.string.Stringifiers;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

import static io.spine.util.Exceptions.unsupported;

/**
 * An envelope holder of the {@linkplain Entity entity} state.
 *
 * @author Alex Tymchenko
 */
public final class EntityStateEnvelope<I, S extends Message>
        implements MessageEnvelope<Any, Entity<I, S>, Empty> {

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
     * The ID of tenant, in scope of which the entity exists.
     */
    private final TenantId tenantId;

    /**
     * The optional version of an entity.
     *
     * <p>The value is only present If the entity used for the envelope construction
     * is a {@link VersionableEntity}. Otherwise, this field is {@code null}.
     */
    private final @Nullable Version entityVersion;

    private EntityStateEnvelope(Entity<I, S> entity, TenantId tenantId) {
        this(entity.getId(), entity.getState(),
             tenantId,
             entity instanceof VersionableEntity
                    ? ((VersionableEntity) entity).getVersion()
                    : null);
    }

    private EntityStateEnvelope(I entityId, S entityState,
                                TenantId tenantId, @Nullable Version entityVersion) {
        this.entityState = entityState;
        this.entityId = Identifier.pack(entityId);
        this.entityStateClass = EntityStateClass.of(entityState);
        this.entityVersion = entityVersion;
        this.tenantId = tenantId;
    }

    public static <I, S extends Message> EntityStateEnvelope of(Entity<I, S> entity,
                                                                TenantId tenantId) {
        return new EntityStateEnvelope<>(entity, tenantId);
    }

    public static <I, S extends Message> EntityStateEnvelope of(I entityId, S entityState,
                                                                @Nullable Version entityVersion,
                                                                TenantId tenantId) {
        return new EntityStateEnvelope<>(entityId, entityState, tenantId, entityVersion);
    }

    @Override
    public Any getId() {
        return entityId;
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
    @Override
    @SuppressWarnings("ReturnOfNull")
    public @Nullable Entity<I, S> getOuterObject() {
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

    @Override
    public Empty getMessageContext() {
        throw unsupported("Entity state does not have context");
    }

    /**
     * This method is not supported and always throws {@link UnsupportedOperationException}.
     *
     * @param builder not used
     * @throws UnsupportedOperationException always
     */
    @Override
    public void setOriginFields(EventContext.Builder builder)
            throws UnsupportedOperationException {
        throw unsupported("An entity state cannot originate other messages");
    }

    /**
     * Obtains the {@link Entity} ID.
     */
    public I getEntityId() {
        I result = Identifier.unpack(entityId);
        return result;
    }

    public Optional<Version> getEntityVersion() {
        return Optional.ofNullable(entityVersion);
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entityState, entityId, entityVersion);
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
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("entityState", Stringifiers.toString(entityState))
                          .add("entityId", Stringifiers.toString(entityId))
                          .add("entityStateClass", entityStateClass)
                          .add("tenantId", Stringifiers.toString(tenantId))
                          .add("entityVersion", entityVersion)
                          .toString();
    }
}
