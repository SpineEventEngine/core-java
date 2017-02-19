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

import com.google.common.base.Converter;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An abstract base for converters of entities into {@link EntityRecord}.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EntityStorageConverter<I, E extends Entity<I, S>, S extends Message>
        extends Converter<E, EntityStorageConverter.Tuple<I>> {

    private final FieldMask fieldMask;

    protected EntityStorageConverter(FieldMask fieldMask) {
        super();
        this.fieldMask = checkNotNull(fieldMask);
    }

    /**
     * Obtains the field mask used by this converter.
     */
    protected FieldMask getFieldMask() {
        return this.fieldMask;
    }

    /**
     * Creates a copy of this converter modified with the passed filed mask.
     */
    public abstract EntityStorageConverter<I, E, S> withFieldMask(FieldMask fieldMask);

    /**
     * Creates new tuple of entity ID and corresponding storage record.
     */
    public static <I> Tuple<I> tuple(I id, EntityRecord record) {
        return new Tuple<>(id, record);
    }

    /**
     * The tuple of an entity ID and a storage record.
     *
     * <p>This data structure is needed for passing entity information to the reverse
     * conversion.
     *
     * @param <I> type of the entity ID
     */
    public static class Tuple<I> extends AbstractEntity<I, EntityRecord> {
        private Tuple(I id, EntityRecord record) {
            super(id);
            injectState(record);
        }
    }
}
