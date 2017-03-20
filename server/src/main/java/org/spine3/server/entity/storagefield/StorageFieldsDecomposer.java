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

package org.spine3.server.entity.storagefield;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.annotations.Internal;
import org.spine3.base.Identifiers;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.StorageFieldType;
import org.spine3.server.entity.StorageFields;
import org.spine3.server.reflect.Getter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * A parser for an {@link Entity} which decomposes it into the {@link StorageFields}.
 *
 * @author Dmytro Dashenkov.
 */
@Internal
class StorageFieldsDecomposer {

    private static final Map<Class, StorageFieldType> STORAGE_FIELD_TYPES;

    static {
        final ImmutableMap.Builder<Class, StorageFieldType> builder = ImmutableMap.builder();
        builder.put(Message.class, StorageFieldType.MESSAGE);
        builder.put(Integer.class, StorageFieldType.INTEGER);
        builder.put(Long.class, StorageFieldType.LONG);
        builder.put(String.class, StorageFieldType.STRING);
        builder.put(Boolean.class, StorageFieldType.BOOLEAN);
        builder.put(Float.class, StorageFieldType.FLOAT);
        builder.put(Double.class, StorageFieldType.DOUBLE);
        STORAGE_FIELD_TYPES = builder.build();
    }

    private final Collection<Getter> getters;

    StorageFieldsDecomposer(Collection<Getter> getters) {
        this.getters = checkNotNull(getters);
    }

    /**
     * Decomposes the given instance of the {@link Entity} by the passed {@link Getter getters}.
     *
     * @param entity the {@link Entity} to decompose
     * @return new instance of the {@link StorageFields} representing passed {@link Entity}
     */
    StorageFields parse(Entity<?, ?> entity) {
        checkNotNull(entity);
        final StorageFields.Builder builder = StorageFields.newBuilder();
        final Object genericId = entity.getId();
        final Any entityId = Identifiers.idToAny(genericId);
        builder.setEntityId(entityId);
        for (Getter getter : getters) {
            final String name = getter.getPropertyName();
            final Object value = getter.get(entity);
            putValue(builder, name, value);
        }

        final StorageFields fields = builder.build();
        return fields;
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyComplexMethod"}) // Since switch over 10 cases
    private static void putValue(StorageFields.Builder builder,
                                 String name,
                                 @Nullable Object value) {
        if (value == null) {
            // TODO:2017-03-16:dmytro.dashenkov: Check this behavior.
            return;
        }
        final Class valueClass = Primitives.wrap(value.getClass());

        final StorageFieldType type = toStorageFieldType(valueClass);
        if (type == null) {
            return;
        }
        switch (type) {
            case MESSAGE:
                final Any any = AnyPacker.pack((Message) value);
                builder.putAnyField(name, any);
                break;
            case INTEGER:
                builder.putIntegerField(name, (Integer) value);
                break;
            case LONG:
                builder.putLongField(name, (Long) value);
                break;
            case STRING:
                builder.putStringField(name, (String) value);
                break;
            case BOOLEAN:
                builder.putBooleanField(name, (Boolean) value);
                break;
            case FLOAT:
                builder.putFloatField(name, (Float) value);
                break;
            case DOUBLE:
                builder.putDoubleField(name, (Double) value);
                break;
            case SFT_UNKNOWN:
            case UNRECOGNIZED:
            default:
                throw new IllegalArgumentException(format(
                        "Cannot add field %s : %s to StorageFields.",
                        name,
                        type));

        }
    }

    @Nullable
    private static StorageFieldType toStorageFieldType(Class cls) {
        final StorageFieldType type = STORAGE_FIELD_TYPES.get(cls);
        if (type != null) {
            return type;
        }
        if (Message.class.isAssignableFrom(cls)) {
            return StorageFieldType.MESSAGE;
        }
        return null;
    }
}
