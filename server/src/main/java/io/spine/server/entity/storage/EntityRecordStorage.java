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
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.QueryParameters;
import io.spine.server.storage.RecordQueries;
import io.spine.server.storage.RecordQuery;
import io.spine.server.storage.RecordStorageDelegate;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.StorageFactory;

import java.util.Iterator;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static io.spine.server.storage.QueryParameters.activeEntityQueryParams;

/**
 * A {@code MessageStorage} which stores {@link EntityRecord}s.
 *
 * @param <I> the type of the identifiers of stored entities
 * @param <S> the type of {@code Entity} state
 */
public class EntityRecordStorage<I, S extends EntityState<I>>
        extends RecordStorageDelegate<I, EntityRecord> {

    private final RecordQuery<I> findActiveRecordsQuery;
    private final QueryParameters activeQueryParams;

    public EntityRecordStorage(StorageFactory factory,
                               Class<? extends Entity<I, ?>> entityClass,
                               boolean multitenant) {
        super(factory.createRecordStorage(spec(entityClass), multitenant));
        activeQueryParams = activeEntityQueryParams(recordSpec());
        this.findActiveRecordsQuery = RecordQueries.of(ImmutableSet.of(), activeQueryParams);
    }

    private static <I> EntityRecordSpec<I> spec(Class<? extends Entity<I, ?>> entityClass) {
        return EntityRecordSpec.of(entityClass);
    }

    /**
     * Returns the iterator over all non-archived and non-deleted entity records.
     *
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<I> index() {
        Iterator<EntityRecord> iterator = readAll(findActiveRecordsQuery);
        return asIdStream(iterator);
    }

    /**
     * Returns the iterator over identifiers of the records which match the passed query.
     *
     * <p>Unless the query specifies otherwise, only non-archived and non-deleted entity records
     * are queried.
     *
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<I> index(RecordQuery<I> query) {
        RecordQuery<I> onlyActive = query.append(findActiveRecordsQuery.getParameters());
        return super.index(onlyActive);
    }

    /**
     * Reads all non-archived and non-deleted entity records according
     * to the response format specified.
     *
     * @param format
     *         the format of the expected response
     * @return iterator over the matching entity records
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<EntityRecord> readAll(ResponseFormat format) {
        return readAll(findActiveRecordsQuery, format);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose as a part of the public API.
     */
    @Override
    public Iterator<EntityRecord> readAll(RecordQuery<I> query, ResponseFormat format) {
        return super.readAll(query, format);
    }

//    //TODO:2020-06-06:alex.tymchenko: complete the implementation.
//    public RecordQuery<I> query(EntityColumn<S, ?> column) {
//        recordSpec().find(column.name())
//    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose as a part of the public API.
     */
    @Override
    public Iterator<EntityRecord> readAll(RecordQuery<I> query) {
        return super.readAll(query);
    }

    /**
     * Reads the entity records by the passed record identifiers, limiting the results
     * to non-archived and non-deleted records.
     *
     * @param ids
     *         identifiers of the records of interest
     * @return iterator over the matching entity records
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<EntityRecord> readAll(Iterable<I> ids) {
        //TODO:2020-04-17:alex.tymchenko: do we need to append the params?
        RecordQuery<I> query = RecordQueries.of(ids, activeQueryParams);
//        RecordQuery<I> query = RecordQueries.of(ids);
        return readAll(query);
    }

    /**
     * Returns the iterator over all stored non-archived and non-deleted entity records.
     *
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<EntityRecord> readAll() {
        return readAll(findActiveRecordsQuery);
    }

    /**
     * Writes the given entity record under the given identifier.
     *
     * <p>Completely ignores the entity columns. Use {@link #write(RecordWithColumns)
     * write(RecordWithColumns)} to write the value with the columns.
     *
     * @param id
     *         the ID for the record
     * @param record
     *         the record to store
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public synchronized void write(I id, EntityRecord record) {
        EntityRecordWithColumns<I> wrapped = EntityRecordWithColumns.create(id, record);
        write(wrapped);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose as a part of the public API.
     */
    @Override
    public void write(RecordWithColumns<I, EntityRecord> record) {
        super.write(record);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose as a part of the public API.
     */
    @Override
    public void writeAll(Iterable<? extends RecordWithColumns<I, EntityRecord>> records) {
        super.writeAll(records);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose as a part of the public API.
     */
    @Override
    public boolean delete(I id) {
        return super.delete(id);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides the parent method in sake of the covariance of the returned value.
     */
    @Internal
    @Override
    @SuppressWarnings("unchecked")  // Guaranteed by the generic declaration of `EntityRecordSpec`.
    public final EntityRecordSpec<I> recordSpec() {
        return (EntityRecordSpec<I>) super.recordSpec();
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
