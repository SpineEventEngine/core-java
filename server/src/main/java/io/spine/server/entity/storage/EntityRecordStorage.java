/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.protobuf.FieldMask;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.EntityQuery;
import io.spine.query.Query;
import io.spine.query.QueryPredicate;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import io.spine.query.Subject;
import io.spine.server.ContextSpec;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.RecordStorageDelegate;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.StorageFactory;

import java.util.Iterator;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static io.spine.server.entity.storage.EntityRecordColumn.archived;
import static io.spine.server.entity.storage.EntityRecordColumn.deleted;
import static io.spine.server.entity.storage.ToEntityRecordQuery.transform;

/**
 * A storage for {@link EntityRecord}s.
 *
 * <p>Delegates all storage operations to the underlying
 * {@link io.spine.server.storage.RecordStorage RecordStorage}. For that, creates a new
 * {@code RecordStorage} instance through the provided {@code StorageFactory} and configures
 * it with the {@linkplain EntityRecordSpec record specification} corresponding
 * to the stored Entity.
 *
 * @param <I>
 *         the type of the identifiers of stored entities
 * @param <S>
 *         the type of {@code Entity} state
 */
public class EntityRecordStorage<I, S extends EntityState<I>>
        extends RecordStorageDelegate<I, EntityRecord> {

    /**
     * The query which aims to find only the records, which entity origins are neither archived
     * nor deleted.
     */
    private final RecordQuery<I, EntityRecord> findActiveRecordsQuery;

    /**
     * Tells if the entity has {@link EntityRecordColumn#archived archived} column declared.
     */
    private final boolean hasArchivedColumn;

    /**
     * Tells if the entity has {@link EntityRecordColumn#deleted deleted} column declared.
     */
    private final boolean hasDeletedColumn;

    /**
     * Creates a new instance.
     *
     * <p>This constructor takes the specification of the Bounded Context in which the storage
     * is created. It may be used by other framework libraries and SPI users to set properties
     * of an underlying DBMS (such as DB table names), with which this record storage interacts.
     *
     * @param context specification of the Bounded Context in which the created storage will be used
     * @param factory storage factory
     * @param entityClass class of an Entity which data is stored in the created storage
     */
    public EntityRecordStorage(ContextSpec context,
                               StorageFactory factory,
                               Class<? extends Entity<I, S>> entityClass) {
        super(context, factory.createRecordStorage(context, spec(entityClass)));
        this.hasArchivedColumn = hasColumn(archived);
        this.hasDeletedColumn = hasColumn(deleted);
        FindActiveEntites<I, S> entityQuery = findActiveEntities().build();
        this.findActiveRecordsQuery = transform(entityQuery);
    }

    private static <I, S extends EntityState<I>> EntityRecordSpec<I, S, ?>
    spec(Class<? extends Entity<I, S>> entityClass) {
        return EntityRecordSpec.of(entityClass);
    }

    /**
     * Returns the iterator over all active entity records.
     *
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<I> index() {
        Iterator<EntityRecord> iterator = super.readAll(findActiveRecordsQuery);
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
    public Iterator<I> index(EntityQuery<I, S, ?> query) {
        RecordQuery<I, EntityRecord> recordQuery = transform(query);
        return index(recordQuery);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the records for both active and non-active entities.
     */
    @Override
    public Iterator<EntityRecord> readAll(Iterable<I> ids, FieldMask mask) {
        return super.readAll(ids, mask);
    }

    /**
     * Reads the entity records according to the passed query.
     *
     * <p>If the passed query has no ID parameter set, only the records of the active entities
     * are returned. If the query includes the ID parameter, includes non-active entities as well.
     */
    @Override
    public Iterator<EntityRecord> readAll(RecordQuery<I, EntityRecord> query) {
        RecordQuery<I, EntityRecord> toExecute = onlyActive(query);
        return super.readAll(toExecute);
    }

    /**
     * Returns the iterator over all stored non-archived and non-deleted entity records.
     *
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<EntityRecord> readAll() {
        return super.readAll(findActiveRecordsQuery);
    }

    /**
     * Finds all records which match the given query.
     *
     * <p>Only the records of active entities are returned.
     */
    public final Iterator<EntityRecord> findAll(EntityQuery<I, S, ?> query) {
        RecordQuery<I, EntityRecord> result = transform(query);
        return readAll(result);
    }

    /**
     * Reads the entity records by the passed record identifiers.
     *
     * <p>The results include the records of both active and non-active entities.
     *
     * @param ids
     *         identifiers of the records of interest
     * @return iterator over the matching entity records
     * @throws IllegalStateException
     *         if the storage is already closed
     */
    @Override
    public Iterator<EntityRecord> readAll(Iterable<I> ids) {
        return super.readAll(ids);
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
    public final EntityRecordSpec<I, S, ?> recordSpec() {
        return (EntityRecordSpec<I, S, ?>) super.recordSpec();
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

    @SuppressWarnings("unchecked")  // ensured by the `EntityClass` definition.
    private Class<S> stateType() {
        return (Class<S>) recordSpec().entityClass()
                                      .stateClass();
    }


    @SuppressWarnings("unchecked")  // ensured by the `Entity` declaration.
    private Class<I> idType() {
        return (Class<I>) recordSpec().entityClass().idClass();
    }

    private static boolean hasNoIds(Query<?, ?> query) {
        return query.subject()
                    .id()
                    .values()
                    .isEmpty();
    }

    private static <I> boolean hasNoLifecycleCols(Query<I, ?> query) {
        Subject<I, ?> subject = query.subject();
        ImmutableList<? extends QueryPredicate<?>> predicates = subject.predicates();
        for (QueryPredicate<?> predicate : predicates) {
            boolean hasColumn = predicate.parameters()
                                         .stream()
                                         .anyMatch((param) -> isLifecycleColumn(param.column()));
            if (hasColumn) {
                return false;
            }
        }
        return true;
    }

    private RecordQuery<I, EntityRecord> onlyActive(RecordQuery<I, EntityRecord> query) {
        RecordQuery<I, EntityRecord> result = query;
        if (hasNoIds(query) && hasNoLifecycleCols(query)) {
            RecordQueryBuilder<I, EntityRecord> builder = query.toBuilder();
            if(hasArchivedColumn) {
                builder.where(archived.asRecordColumn(Boolean.class)).is(false);
            }
            if(hasDeletedColumn) {
                builder.where(deleted.asRecordColumn(Boolean.class)).is(false);
            }
            result = builder.build();
        }
        return result;
    }

    private static boolean isLifecycleColumn(Column<?, ?> column) {
        ColumnName name = column.name();
        return name.equals(archived.columnName()) || name.equals(deleted.columnName());
    }

    private FindActiveEntites.Builder<I, S> findActiveEntities() {
        return FindActiveEntites.newBuilder(idType(), stateType(),
                                            hasArchivedColumn, hasDeletedColumn);
    }

    private boolean hasColumn(EntityRecordColumn column) {
        EntityRecordSpec<I, S, ?> spec = recordSpec();
        return spec.findColumn(column.columnName())
                              .isPresent();
    }
}
