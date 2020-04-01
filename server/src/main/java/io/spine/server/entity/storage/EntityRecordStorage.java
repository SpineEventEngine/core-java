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

package io.spine.server.entity.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.base.Identifier;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.MessageQuery;
import io.spine.server.storage.MessageStorageDelegate;
import io.spine.server.storage.MessageWithColumns;
import io.spine.server.storage.StorageFactory;

import java.util.Iterator;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static io.spine.server.entity.storage.QueryParameters.activeEntityQueryParams;

/**
 * A {@code MessageStorage} which stores {@link EntityRecord}s.
 */
public class EntityRecordStorage<I> extends MessageStorageDelegate<I, EntityRecord> {

    private final MessageQuery<I> findActiveRecordsQuery;
    private final QueryParameters activeQueryParams;

    public EntityRecordStorage(StorageFactory factory,
                               Class<? extends Entity<I, ?>> entityClass,
                               boolean multitenant) {
        super(factory.createMessageStorage(EntityColumns.of(entityClass), multitenant));
        activeQueryParams = activeEntityQueryParams(columns());
        this.findActiveRecordsQuery = MessageQuery.of(ImmutableSet.of(), activeQueryParams);
    }

    /**
     * Returns the iterator over all non-archived and non-deleted entity records.
     */
    @Override
    public Iterator<I> index() {
        Iterator<EntityRecord> iterator = readAll(findActiveRecordsQuery);
        return asIdStream(iterator);
    }

    @Override
    public synchronized void write(I id, EntityRecord record) {
        EntityRecordWithColumns<I> withLifecycleCols = EntityRecordWithColumns.create(id, record);
        write(withLifecycleCols);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the parent method in order to expose it as a part of public API.
     */
    @Override
    //TODO:2020-03-31:alex.tymchenko: do we take `EntityRecordWithColumns` instead?
    public void write(MessageWithColumns<I, EntityRecord> record) {
        super.write(record);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the parent method in order to expose it as a part of public API.
     */
    @Override
    public void writeAll(Iterable<? extends MessageWithColumns<I, EntityRecord>> records) {
        super.writeAll(records);
    }

    /**
     * Reads all non-archived and non-deleted entity records according
     * to the response format specified.
     *
     * @param format
     *         the format of the expected response
     * @return iterator over the matching entity records
     */
    @Override
    public Iterator<EntityRecord> readAll(ResponseFormat format) {
        return readAll(findActiveRecordsQuery, format);
    }

    /**
     * Reads the entity records by the passed record identifiers, limiting the results
     * to non-archived and non-deleted records.
     *
     * @param ids
     *         identifiers of the records of interest
     * @return iterator over the matching entity records
     */
    @Override
    public Iterator<EntityRecord> readAll(Iterable<I> ids) {
        MessageQuery<I> query = MessageQuery.of(ids, activeQueryParams);
        return readAll(query);
    }

    /**
     * Returns the iterator over all stored non-archived and non-deleted entity records.
     */
    @Override
    public Iterator<EntityRecord> readAll() {
        return readAll(findActiveRecordsQuery);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the parent method in sake of the covariance of the returned value.
     */
    @Internal
    @Override
    public final EntityColumns columns() {
        return (EntityColumns) super.columns();
    }

    /**
     * Transforms the iterator over the entity records into the iterator over their identifiers.
     *
     * @param iterator
     *         source iterator
     * @return iterator over the identifiers of the passed {@code EntityRecord}s
     */
    @SuppressWarnings("unchecked")
    private Iterator<I> asIdStream(Iterator<EntityRecord> iterator) {
        ImmutableList<I> ids = stream(iterator)
                .map(record -> Identifier.unpack(record.getEntityId()))
                .map(id -> (I) id)
                .collect(toImmutableList());
        return ids.iterator();
    }
}
