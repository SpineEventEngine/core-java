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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.type.TypeUrl;

/**
 * Default implementation of {@code EntityStorageConverter} for {@code AbstractEntity}.
 *
 * @param <I> the type of entity IDs
 * @param <E> the type of entities
 * @param <S> the type of entity states
 * @author Alexander Yevsyukov
 */
class DefaultEntityStorageConverter<I, E extends AbstractEntity<I, S>, S extends Message>
        extends EntityStorageConverter<I, E, S> {

    private static final long serialVersionUID = 0L;

    private DefaultEntityStorageConverter(TypeUrl entityStateType,
                                          EntityFactory<I, E> factory,
                                          FieldMask fieldMask) {
        super(entityStateType, factory, fieldMask);
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
        return new DefaultEntityStorageConverter<>(getEntityStateType(),
                                                   getEntityFactory(),
                                                   fieldMask);
    }

    /**
     * Sets lifecycle flags in the builder from the entity, if the entity is
     * {@linkplain AbstractVersionableEntity versionable}.
     *
     * @param builder the entity builder to update
     * @param entity  the entity which data is passed to the {@link EntityRecord} we are building
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Override
    protected void updateBuilder(EntityRecord.Builder builder, E entity) {
        if (entity instanceof AbstractVersionableEntity) {
            AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
            builder.setVersion(versionable.getVersion())
                   .setLifecycleFlags(versionable.getLifecycleFlags());
        }
    }

    /**
     * Injects the state into an entity.
     *
     * <p>if the passed entity is versionable its lifecycle flags will be set from the record.
     *
     * <p>If not, {@code IllegalStateException} is thrown suggesting to provide a custom
     * {@link EntityStorageConverter} in the repository which manages entities of this class.
     *
     * @param entity       the entity to inject the state
     * @param state        the state message to inject
     * @param entityRecord the {@link EntityRecord} which contains additional attributes that may be
     *                     injected
     */
    @SuppressWarnings({
            "ChainOfInstanceofChecks" /* `DefaultEntityStorageConverter` supports conversion of
                entities derived from standard abstract classes. A custom `Entity` class needs a
                custom state injection. We do not want to expose state injection in the `Entity`
                interface.*/,
            "unchecked" /* The state type is the same as the parameter of this class. */})
    @Override
    protected void injectState(E entity, S state, EntityRecord entityRecord) {
        if (entity instanceof AbstractVersionableEntity) {
            AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
            versionable.updateState(state, entityRecord.getVersion());
            versionable.setLifecycleFlags(entityRecord.getLifecycleFlags());
        } else {
            entity.setState(state);
        }
    }
}
