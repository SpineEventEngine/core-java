/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import org.spine3.SPI;
import org.spine3.server.EntityId;

import javax.annotation.Nullable;

import static org.spine3.util.Identifiers.idToString;

/**
 * An entity storage keeps messages with identity.
 *
 * <p>See {@link EntityId} for supported ID types.
 *
 * @param <I> the type of entity IDs
 * @author Alexander Yevsyukov
 */
@SPI
@SuppressWarnings("ClassMayBeInterface")
public abstract class EntityStorage<I> {

    /**
     * Loads an entity storage record from the storage by an ID.
     *
     * @param id the ID of the entity record to load
     * @return an entity record instance or {@code null} if there is no record with such an ID
     */
    @Nullable
    public abstract EntityStorageRecord read(I id);

    /**
     * Writes a record into the storage. Rewrites it if a record with such an entity ID already exists.
     *
     * @param record a record to save
     * @throws NullPointerException if the {@code record} is null
     */
    public abstract void write(EntityStorageRecord record);

    /**
     * Converts an entity ID to a storage record ID with the string ID representation or number ID value.
     *
     * @param id an ID to convert
     * @see EntityId
     */
    public static <I> EntityStorageRecord.Id toRecordId(I id) {
        final EntityStorageRecord.Id.Builder builder = EntityStorageRecord.Id.newBuilder();
        //noinspection ChainOfInstanceofChecks
        if (id instanceof Long) {
            builder.setLongValue((Long) id);
        } else if (id instanceof Integer) {
            builder.setIntValue((Integer) id);
        } else {
            final String stringId = idToString(id);
            builder.setStringValue(stringId);
        }
        return builder.build();
    }
}
