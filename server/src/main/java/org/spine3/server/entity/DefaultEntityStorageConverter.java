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

    private final Repository<I, E> repository;
    private final FieldMask fieldMask;

    static <I, E extends AbstractEntity<I, S>, S extends Message>
    EntityStorageConverter<I, E, S> forAllFields(Repository<I, E> repository) {
        return new DefaultEntityStorageConverter<>(repository, FieldMask.getDefaultInstance());
    }

    private DefaultEntityStorageConverter(Repository<I, E> repository, FieldMask fieldMask) {
        super(fieldMask);
        this.repository = repository;
        this.fieldMask = fieldMask;
    }

    @Override
    public EntityStorageConverter<I, E, S> withFieldMask(FieldMask fieldMask) {
        return new DefaultEntityStorageConverter<>(this.repository, fieldMask);
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
        final TypeUrl entityStateType = repository.getEntityStateType();
        final S state = (S) FieldMasks.applyMask(fieldMask, unpacked, entityStateType);

        final I id = (I) idFromAny(entityRecord.getEntityId());
        final E entity = repository.create(id);

        if (entity != null) {
            if (entity instanceof AbstractVersionableEntity) {
                final AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
                versionable.setState(state, entityRecord.getVersion());
                versionable.setLifecycleFlags(entityRecord.getLifecycleFlags());
            } else {
                entity.injectState(state);
            }
        }
        return entity;
    }
}
