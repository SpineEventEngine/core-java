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

import com.google.protobuf.Any;
import org.spine3.base.Identifiers;
import org.spine3.server.entity.Entity;
import org.spine3.server.entity.StorageFields;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.base.Messagifiers.toAny;

/**
 * @author Dmytro Dashenkov.
 */
class StorageFieldsGenerator<E extends Entity<?, ?>> {

    private final Collection<EntityFieldGetter<E>> descriptors;

    StorageFieldsGenerator(Collection<EntityFieldGetter<E>> descriptors) {
        this.descriptors = checkNotNull(descriptors);
    }

    public StorageFields generate(E entity) {
        checkNotNull(entity);
        final Map<String, Any> properties = new HashMap<>(descriptors.size());
        for (EntityFieldGetter<E> descriptor : descriptors) {
            final String name = descriptor.getName();
            final Object value = descriptor.get(entity);
            final Any anyValue = toAny(value);
            properties.put(name, anyValue);
        }
        final Object genericId = entity.getId();
        final Any id = Identifiers.idToAny(genericId);

        final StorageFields fields = StorageFields.newBuilder()
                                                  .setEntityId(id)
                                                  .putAllFields(properties)
                                                  .build();
        return fields;
    }
}
