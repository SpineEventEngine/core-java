/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.base.Converter;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * An abstract base for converters of entities into {@link EntityRecord}.
 */
public abstract class StorageConverter<I, E extends Entity<I, S>, S extends Message>
        extends Converter<E, EntityRecord> implements Serializable {

    private static final long serialVersionUID = 0L;
    private final TypeUrl entityStateType;
    private final EntityFactory<E> entityFactory;
    private final FieldMask fieldMask;

    protected StorageConverter(TypeUrl entityStateType,
                               EntityFactory<E> factory,
                               FieldMask fieldMask) {
        super();
        this.fieldMask = checkNotNull(fieldMask);
        this.entityFactory = factory;
        this.entityStateType = entityStateType;
    }

    /**
     * Obtains the type URL of the state of entities which this converter builds.
     */
    protected TypeUrl entityStateType() {
        return entityStateType;
    }

    /**
     * Obtains the entity factory used by the converter.
     */
    protected EntityFactory<E> entityFactory() {
        return entityFactory;
    }

    /**
     * Obtains the field mask used by this converter to trim the state of entities before the state
     * is {@linkplain #injectState(Entity, Message, EntityRecord) injected} into entities.
     */
    protected FieldMask fieldMask() {
        return this.fieldMask;
    }

    /**
     * Creates a copy of this converter modified with the passed filed mask.
     */
    public abstract StorageConverter<I, E, S> withFieldMask(FieldMask fieldMask);

    @Override
    protected EntityRecord doForward(E entity) {
        Any entityId = Identifier.pack(entity.id());
        Any stateAny = pack(entity.state());
        EntityRecord.Builder builder = EntityRecord
                .newBuilder()
                .setEntityId(entityId)
                .setState(stateAny);
        updateBuilder(builder, entity);
        return builder.build();
    }

    @SuppressWarnings("unchecked" /* The cast is safe since the <I> and <S> types are bound with
            the type <E>, and forward conversion is performed on the entity of type <E>. */)
    @Override
    protected E doBackward(EntityRecord entityRecord) {
        S unpacked = (S) unpack(entityRecord.getState());
        S state = FieldMasks.applyMask(fieldMask(), unpacked);
        I id = (I) Identifier.unpack(entityRecord.getEntityId());
        E entity = entityFactory.create(id);
        checkState(entity != null, "EntityFactory produced null entity.");
        injectState(entity, state, entityRecord);
        return entity;
    }

    /**
     * Derived classes may override to additionally tune the passed entity builder.
     *
     * <p>Default implementation does nothing.
     *
     * @param builder the entity builder to update
     * @param entity  the entity which data is passed to the {@link EntityRecord} we are building
     */
    @SuppressWarnings("NoopMethodInAbstractClass") // Avoid forcing empty implementations.
    protected void updateBuilder(EntityRecord.Builder builder, E entity) {
        // Do nothing.
    }

    /**
     * Derived classes must implement providing state injection into the passed entity.
     *
     * @param entity       the entity into which inject the state
     * @param state        the state message extracted from the record
     * @param entityRecord the record which may contain additional properties for the entity
     */
    protected abstract void injectState(E entity, S state, EntityRecord entityRecord);

    @Override
    public int hashCode() {
        return Objects.hash(entityFactory, entityStateType, fieldMask);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StorageConverter)) {
            return false;
        }
        StorageConverter other = (StorageConverter) obj;
        return Objects.equals(this.entityStateType, other.entityStateType)
                && Objects.equals(this.entityFactory, other.entityFactory)
                && Objects.equals(this.fieldMask, other.fieldMask);
    }
}
