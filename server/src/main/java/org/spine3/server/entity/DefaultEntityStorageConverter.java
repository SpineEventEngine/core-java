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
import org.spine3.protobuf.TypeUrl;

import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * Default implementation of {@code EntityStorageConverter} for {@code AbstractVersionableEntity}.
 *
 * @author Alexander Yevsyukov
 * @see Tuple
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
    protected Tuple<I> doForward(E entity) {
        final Any stateAny = pack(entity.getState());
        final EntityRecord.Builder builder =
                EntityRecord.newBuilder()
                            .setState(stateAny);

        if (entity instanceof AbstractVersionableEntity) {
            AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
            builder.setVersion(versionable.getVersion())
                   .setVisibility(versionable.getVisibility());
        }

        final Tuple<I> result = tuple(entity.getId(), builder.build());
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected E doBackward(Tuple<I> tuple) {
        final Message unpacked = unpack(tuple.getState()
                                             .getState());
        final TypeUrl entityStateType = repository.getEntityStateType();
        final S state = (S) FieldMasks.applyMask(fieldMask, unpacked, entityStateType);

        final E entity = repository.create(tuple.getId());

        final EntityRecord record = tuple.getState();
        if (entity != null) {
            if (entity instanceof AbstractVersionableEntity) {
                AbstractVersionableEntity versionable = (AbstractVersionableEntity) entity;
                versionable.setState(state, record.getVersion());
                versionable.setVisibility(record.getVisibility());
            } else {
                entity.injectState(state);
            }
        }
        return entity;
    }
}
