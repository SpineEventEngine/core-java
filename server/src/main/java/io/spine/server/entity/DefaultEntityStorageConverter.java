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

import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.type.TypeUrl;

/**
 * Default implementation of {@code EntityStorageConverter} for {@code AbstractEntity}.
 *
 * @param <I> the type of entity IDs
 * @param <E> the type of entities
 * @param <S> the type of entity states
 */
final class DefaultEntityStorageConverter<I, E extends AbstractEntity<I, S>, S extends Message>
        extends EntityStorageConverter<I, E, S> {

    private static final long serialVersionUID = 0L;

    private DefaultEntityStorageConverter(TypeUrl stateType,
                                          EntityFactory<I, E> factory,
                                          FieldMask fieldMask) {
        super(stateType, factory, fieldMask);
    }

    static <I, E extends AbstractEntity<I, S>, S extends Message>
    EntityStorageConverter<I, E, S> forAllFields(TypeUrl stateType, EntityFactory<I, E> factory) {
        FieldMask allFields = FieldMask.getDefaultInstance();
        return new DefaultEntityStorageConverter<>(stateType, factory, allFields);
    }

    @Override
    public EntityStorageConverter<I, E, S> withFieldMask(FieldMask fieldMask) {
        TypeUrl stateType = entityStateType();
        EntityFactory<I, E> factory = entityFactory();
        return new DefaultEntityStorageConverter<>(stateType, factory, fieldMask);
    }

    /**
     * Sets lifecycle flags in the builder from the entity.
     *
     * @param builder
     *         the entity builder to update
     * @param entity
     *         the entity which data is passed to the {@link EntityRecord} we are building
     */
    @SuppressWarnings("CheckReturnValue") // calling builder
    @Override
    protected void updateBuilder(EntityRecord.Builder builder, E entity) {
        builder.setVersion(entity.version())
               .setLifecycleFlags(entity.getLifecycleFlags());
    }

    /**
     * Injects the state into an entity.
     *
     * @param entity
     *         the entity to inject the state
     * @param state
     *         the state message to inject
     * @param entityRecord
     *         the {@link EntityRecord} which contains additional attributes that may be injected
     */
    @Override
    protected void injectState(E entity, S state, EntityRecord entityRecord) {
        entity.updateState(state, entityRecord.getVersion());
        entity.setLifecycleFlags(entityRecord.getLifecycleFlags());
    }
}
