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

import com.google.protobuf.Any;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.client.EntityIdVBuilder;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.EntityHistoryIdVBuilder;
import io.spine.type.TypeUrl;

import static io.spine.base.Identifier.pack;

/**
 * Utilities for working with {@linkplain io.spine.system.server.EntityHistoryId entity history
 * identifiers}.
 */
@Internal
public final class EntityHistoryIds {

    /** Prevents instantiation of this utility class. */
    private EntityHistoryIds() {
    }

    /**
     * Obtains the entity history ID from the given entity ID.
     *
     * @param id
     *         the ID of the entity
     * @param entityType
     *         the type of the entity
     * @param <T>
     *         the type of the ID class
     * @return the {@link EntityHistoryId}
     */
    public static <T> EntityHistoryId wrap(T id, TypeUrl entityType) {
        EntityId entityId = EntityIdVBuilder
                .newBuilder()
                .setId(pack(id))
                .build();
        EntityHistoryId historyId = EntityHistoryIdVBuilder
                .newBuilder()
                .setEntityId(entityId)
                .setTypeUrl(entityType.value())
                .build();
        return historyId;
    }

    /**
     * Obtains the entity ID from the given {@link EntityHistoryId}.
     *
     * @param historyId
     *         the ID of the entity history
     * @return the extracted entity ID
     */
    public static Object unwrap(EntityHistoryId historyId) {
        Any idValue = historyId.getEntityId()
                               .getId();
        Object unpackedId = Identifier.unpack(idValue);
        return unpackedId;
    }
}
