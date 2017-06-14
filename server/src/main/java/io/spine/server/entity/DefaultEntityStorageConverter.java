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

package io.spine.server.entity;

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.Objects;

import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Default implementation of {@code EntityStorageConverter} for {@code AbstractEntity}.
 *
 * @author Alexander Yevsyukov
 */
class DefaultEntityStorageConverter<I, E extends Entity<I, S>, S extends Message>
        extends EntityStorageConverter<I, E, S> {

    private static final long serialVersionUID = 0L;
    private final EntityFactory<I, E> entityFactory;
    private final TypeUrl entityStateType;
    private final FieldMask fieldMask;

    private DefaultEntityStorageConverter(TypeUrl entityStateType,
                                          EntityFactory<I, E> factory,
                                          FieldMask fieldMask) {
        super(fieldMask);
        this.entityStateType = entityStateType;
        this.entityFactory = factory;
        this.fieldMask = fieldMask;
    }

    static <I, E extends AbstractEntity<I, S>, S extends Message>
    EntityStorageConverter<I, E, S> forAllFields(TypeUrl entityStateType,
                                                 EntityFactory<I, E> factory) {
        return new DefaultEntityStorageConverter<>(entityStateType,
                                                   factory,
                                                   FieldMask.getDefaultInstance());
    }

    @Override
    public EntityStorageConverter<I, E, S> withFieldMask(FieldMask fieldMask) {
        return new DefaultEntityStorageConverter<>(entityStateType, entityFactory, fieldMask);
    }

    @Override
    protected EntityRecord doForward(E entity) {
        final Any entityId = Identifier.pack(entity.getId());
        final Any stateAny = pack(entity.getState());
        final EntityRecord.Builder builder = EntityRecord.newBuilder()
                                                         .setEntityId(entityId)
                                                         .setState(stateAny);
        if (entity instanceof AbstractVersionableEntity) {
            final AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
            builder.setVersion(versionable.getVersion())
                   .setLifecycleFlags(versionable.getLifecycleFlags());
        }

        return builder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected E doBackward(EntityRecord entityRecord) {
        final Message unpacked = unpack(entityRecord.getState());
        final S state = (S) FieldMasks.applyMask(fieldMask, unpacked, entityStateType);

        final I id = (I) Identifier.unpack(entityRecord.getEntityId());
        final E entity = entityFactory.create(id);

        if (entity != null) {
            injectState(entity, state, entityRecord);
        }
        return entity;
    }

    /**
     * Injects the state into an entity.
     *
     * <p>The method attempts to cast the passed entity instance into one of the standard abstract
     * implementations provided by the framework. If successful, it invokes corresponding state
     * mutation method(s).
     *
     * <p>If not, {@code IllegalStateException} is thrown suggesting to provide a custom
     * {@link EntityStorageConverter} in the repository which manages entities of this class.
     *
     * @param entity       the entity to inject the state
     * @param state        the state message to inject
     * @param entityRecord the {@link EntityRecord} which contains additional attributes that may be
     *                     injected
     * @throws IllegalStateException if the passed entity instance is implemented outside of the
     *                               framework
     */
    @SuppressWarnings({
            "ChainOfInstanceofChecks" /* `DefaultEntityStorageConverter` supports conversion of
                entities derived from standard abstract classes. A custom `Entity` class needs a
                custom state injection. We do not want to expose state injection in the `Entity`
                interface.*/,
            "unchecked" /* The state type is the same as the parameter of this class. */})
    private void injectState(E entity, S state, EntityRecord entityRecord) {
        if (entity instanceof AbstractVersionableEntity) {
            final AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
            versionable.updateState(state, entityRecord.getVersion());
            versionable.setLifecycleFlags(entityRecord.getLifecycleFlags());
        } else if (entity instanceof AbstractEntity) {
            ((AbstractEntity) entity).setState(state);
        } else {
            throw newIllegalStateException(
                    "Cannot inject state into an Entity of the class %s. " +
                            "Please set custom EntityStorageConverter into the repository " +
                            "managing entities of this class.",
                    entity.getClass()
                          .getName());
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DefaultEntityStorageConverter other = (DefaultEntityStorageConverter) obj;
        return Objects.equals(this.entityFactory, other.entityFactory)
                && Objects.equals(this.entityStateType, other.entityStateType)
                && Objects.equals(this.fieldMask, other.fieldMask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityFactory, entityStateType, fieldMask);
    }
}
