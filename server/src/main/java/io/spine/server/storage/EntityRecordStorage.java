/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityColumns;
import io.spine.server.entity.storage.QueryParameters;

import java.util.Iterator;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static io.spine.server.entity.storage.QueryParameters.activeEntityQueryParams;

/**
 * A {@code MessageStorage} which stores {@link EntityRecord}s.
 */
public class EntityRecordStorage<I> extends MessageStorageDelegate<I, EntityRecord> {

    private final MessageQuery<I> findActiveRecordsQuery;

    EntityRecordStorage(StorageFactory factory,
                        Class<? extends Entity<I, ?>> entityClass,
                        boolean multitenant) {
        super(factory.createMessageStorage(EntityColumns.of(entityClass), multitenant));
        QueryParameters params = activeEntityQueryParams(columns());
        this.findActiveRecordsQuery = MessageQuery.of(ImmutableSet.of(), params);
    }

    @Internal
    @Override
    public final EntityColumns columns() {
        return (EntityColumns) super.columns();
    }

    @Override
    public Iterator<I> index() {
        Iterator<EntityRecord> iterator = readAll(findActiveRecordsQuery);
        return asIdStream(iterator);
    }

    @Override
    public Iterator<EntityRecord> readAll(ResponseFormat format) {
        return readAll(findActiveRecordsQuery, format);
    }

    @SuppressWarnings("unchecked")
    private Iterator<I> asIdStream(Iterator<EntityRecord> iterator) {
        ImmutableList<I> ids = stream(iterator)
                .map(record -> Identifier.unpack(record.getEntityId()))
                .map(id -> (I) id)
                .collect(toImmutableList());
        return ids.iterator();
    }
}
