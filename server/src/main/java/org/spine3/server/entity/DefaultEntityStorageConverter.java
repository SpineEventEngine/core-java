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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.spine3.type.TypeUrl;

import static org.spine3.base.Identifiers.idFromAny;
import static org.spine3.base.Identifiers.idToAny;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * Default implementation of {@code EntityStorageConverter} for {@code AbstractVersionableEntity}.
 *
 * @author Alexander Yevsyukov
 */
class DefaultEntityStorageConverter<I, E extends AbstractEntity<I, S>, S extends Message>
        extends EntityStorageConverter<I, E, S> {

    private static final long serialVersionUID = 0L;
    private final EntityFactory<I, E> entityFactory;
    private final TypeUrl entityStateType;
    private final FieldMask fieldMask;

    static <I, E extends AbstractEntity<I, S>, S extends Message>
    EntityStorageConverter<I, E, S> forAllFields(TypeUrl entityStateType,
                                                 EntityFactory<I, E> factory) {
        return new DefaultEntityStorageConverter<>(entityStateType,
                                                   factory,
                                                   FieldMask.getDefaultInstance());
    }

    private DefaultEntityStorageConverter(TypeUrl entityStateType,
                                          EntityFactory<I, E> factory,
                                          FieldMask fieldMask) {
        super(fieldMask);
        this.entityStateType = entityStateType;
        this.entityFactory = factory;
        this.fieldMask = fieldMask;
    }

    @Override
    public EntityStorageConverter<I, E, S> withFieldMask(FieldMask fieldMask) {
        return new DefaultEntityStorageConverter<>(entityStateType, entityFactory, fieldMask);
    }

    @Override
    protected EntityRecord doForward(E entity) {
        final Any entityId = idToAny(entity.getId());
        final Any stateAny = pack(entity.getState());
        final EntityRecord.Builder builder =
                EntityRecord.newBuilder()
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

        final I id = (I) idFromAny(entityRecord.getEntityId());
        final E entity = entityFactory.create(id);

        if (entity != null) {
            if (entity instanceof AbstractVersionableEntity) {
                final AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
                versionable.updateState(state, entityRecord.getVersion());
                versionable.setLifecycleFlags(entityRecord.getLifecycleFlags());
            } else {
                entity.injectState(state);
            }
        }
        return entity;
    }
}
