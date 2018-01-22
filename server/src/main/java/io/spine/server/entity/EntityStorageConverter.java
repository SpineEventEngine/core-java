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

package io.spine.server.entity;

import com.google.common.base.Converter;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * An abstract base for converters of entities into {@link EntityRecord}.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EntityStorageConverter<I, E extends Entity<I, S>, S extends Message>
        extends Converter<E, EntityRecord> implements Serializable {

    private static final long serialVersionUID = 0L;
    private final TypeUrl entityStateType;
    private final EntityFactory<I, E> entityFactory;
    private final FieldMask fieldMask;

    protected EntityStorageConverter(TypeUrl entityStateType,
                                     EntityFactory<I, E> factory,
                                     FieldMask fieldMask) {
        super();
        this.fieldMask = checkNotNull(fieldMask);
        this.entityFactory = factory;
        this.entityStateType = entityStateType;
    }

    /**
     * Obtains the type URL of the state of entities which this converter builds.
     */
    protected TypeUrl getEntityStateType() {
        return entityStateType;
    }

    /**
     * Obtains the entity factory used by the converter.
     */
    protected EntityFactory<I, E> getEntityFactory() {
        return entityFactory;
    }

    /**
     * Obtains the field mask used by this converter to trim the state of entities before the state
     * is {@linkplain #injectState(Entity, Message, EntityRecord) injected} into entities.
     */
    protected FieldMask getFieldMask() {
        return this.fieldMask;
    }

    /**
     * Creates a copy of this converter modified with the passed filed mask.
     */
    public abstract EntityStorageConverter<I, E, S> withFieldMask(FieldMask fieldMask);

    @Override
    protected EntityRecord doForward(E entity) {
        final Any entityId = Identifier.pack(entity.getId());
        final Any stateAny = pack(entity.getState());
        final EntityRecord.Builder builder = EntityRecord.newBuilder()
                                                         .setEntityId(entityId)
                                                         .setState(stateAny);
        updateBuilder(builder, entity);
        return builder.build();
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

    @Override
    @SuppressWarnings("unchecked")
    protected E doBackward(EntityRecord entityRecord) {
        final S unpacked = unpack(entityRecord.getState());
        final S state = FieldMasks.applyMask(getFieldMask(), unpacked, entityStateType);

        final I id = Identifier.unpack(entityRecord.getEntityId());
        final E entity = entityFactory.create(id);

        if (entity != null) {
            injectState(entity, state, entityRecord);
        }
        return entity;
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
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EntityStorageConverter other = (EntityStorageConverter) obj;
        return Objects.equals(this.entityStateType, other.entityStateType)
                && Objects.equals(this.entityFactory, other.entityFactory)
                && Objects.equals(this.fieldMask, other.fieldMask);
    }
}
