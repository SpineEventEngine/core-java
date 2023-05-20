/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.client.Targets;
import io.spine.core.Version;
import io.spine.protobuf.TypeConverter;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity.PROJECT_VERSION_TIMESTAMP;
import static io.spine.server.storage.given.RecordStorageTestEnv.archive;
import static io.spine.server.storage.given.RecordStorageTestEnv.assertSingleRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.buildStorageRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.delete;
import static io.spine.server.storage.given.RecordStorageTestEnv.emptyFilters;
import static io.spine.server.storage.given.RecordStorageTestEnv.newEntity;
import static io.spine.test.storage.Project.Status.CANCELLED;
import static io.spine.test.storage.Project.Status.DONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("unused") // JUnit nested classes considered unused in abstract class.
public abstract class RecordStorageTest<S extends RecordStorage<ProjectId>>
        extends AbstractRecordStorageTest<ProjectId, S> {

    @Override
    protected Class<? extends Entity<?, ?>> getTestEntityClass() {
        return TestCounterEntity.class;
    }

    @Override
    protected final ProjectId newId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    @Test
    @DisplayName("write record with columns")
    void writeRecordWithColumns() {
        ProjectId id = newId();
        EntityRecord record = newStorageRecord(id);
        Entity<ProjectId, ?> testEntity = newEntity(id);
        RecordStorage<ProjectId> storage = storage();
        EntityRecordWithColumns recordWithColumns =
                EntityRecordWithColumns.create(record, testEntity, storage);
        storage.write(id, recordWithColumns);

        RecordReadRequest<ProjectId> readRequest = newReadRequest(id);
        Optional<EntityRecord> readRecord = storage.read(readRequest);
        assertTrue(readRecord.isPresent());
        assertEquals(record, readRecord.get());
    }

    @Test
    @DisplayName("filter records by columns")
    void filterByColumns() {
        Project.Status requiredValue = DONE;
        Int32Value wrappedValue = Int32Value
                .newBuilder()
                .setValue(requiredValue.getNumber())
                .build();
        Version versionValue = Version
                .newBuilder()
                .setNumber(0)
                .setTimestamp(PROJECT_VERSION_TIMESTAMP)
                .build();

        Filter status = eq("project_status_value", wrappedValue);
        Filter version = eq("project_version", versionValue);
        CompositeFilter aggregatingFilter = CompositeFilter
                .newBuilder()
                .setOperator(ALL)
                .addFilter(status)
                .addFilter(version)
                .build();
        TargetFilters filters = TargetFilters
                .newBuilder()
                .addFilter(aggregatingFilter)
                .build();

        RecordStorage<ProjectId> storage = storage();

        EntityQuery<ProjectId> query = newEntityQuery(filters);
        ProjectId idMatching = newId();
        ProjectId idWrong1 = newId();
        ProjectId idWrong2 = newId();

        TestCounterEntity matchingEntity = newEntity(idMatching);
        TestCounterEntity wrongEntity1 = newEntity(idWrong1);
        TestCounterEntity wrongEntity2 = newEntity(idWrong2);

        // 2 of 3 have required values

        matchingEntity.assignStatus(requiredValue);
        wrongEntity1.assignStatus(requiredValue);
        wrongEntity2.assignStatus(CANCELLED);

        // Change internal Entity state
        wrongEntity1.assignCounter(1);

        // After the mutation above the single matching record is the one under the `idMatching` ID

        EntityRecord fineRecord = buildStorageRecord(idMatching, newState(idMatching));
        EntityRecord notFineRecord1 = buildStorageRecord(idWrong1, newState(idWrong1));
        EntityRecord notFineRecord2 = buildStorageRecord(idWrong2, newState(idWrong2));

        EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity);
        EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1);
        EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2);

        storage.write(idMatching, recordRight);
        storage.write(idWrong1, recordWrong1);
        storage.write(idWrong2, recordWrong2);

        Iterator<EntityRecord> readRecords = storage.readAll(query);
        assertSingleRecord(fineRecord, readRecords);
    }

    private EntityRecordWithColumns create(EntityRecord record, Entity<?, ?> entity) {
        return EntityRecordWithColumns.create(record, entity, storage());
    }

    private EntityQuery<ProjectId> newEntityQuery(TargetFilters filters) {
        return EntityQueries.from(filters, storage());
    }

    @Test
    @DisplayName("filter records by ID and not use columns")
    void filterByIdAndNoColumns() {
        // Create the test data
        ProjectId idMatching = newId();
        ProjectId idWrong1 = newId();
        ProjectId idWrong2 = newId();

        Entity<ProjectId, ?> matchingEntity = newEntity(idMatching);
        Entity<ProjectId, ?> wrongEntity1 = newEntity(idWrong1);
        Entity<ProjectId, ?> wrongEntity2 = newEntity(idWrong2);

        EntityRecord fineRecord = buildStorageRecord(idMatching, newState(idMatching));
        EntityRecord notFineRecord1 = buildStorageRecord(idWrong1, newState(idWrong1));
        EntityRecord notFineRecord2 = buildStorageRecord(idWrong2, newState(idWrong2));

        RecordStorage<ProjectId> storage = storage();

        EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity);
        EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1);
        EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2);

        // Fill the storage
        storage.write(idWrong1, recordWrong1);
        storage.write(idMatching, recordRight);
        storage.write(idWrong2, recordWrong2);

        // Prepare the query
        Any entityId = TypeConverter.toAny(idMatching);
        TargetFilters filters = Targets.acceptingOnly(entityId);
        EntityQuery<ProjectId> query = newEntityQuery(filters);

        // Perform the query
        Iterator<EntityRecord> readRecords = storage.readAll(query);
        // Check results
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    @DisplayName("read archived records if specified")
    void readArchivedRecords() {
        ProjectId activeRecordId = newId();
        ProjectId archivedRecordId = newId();

        EntityRecord activeRecord =
                buildStorageRecord(activeRecordId, newState(activeRecordId));
        EntityRecord archivedRecord =
                buildStorageRecord(archivedRecordId, newState(archivedRecordId));
        TransactionalEntity<ProjectId, ?, ?> activeEntity = newEntity(activeRecordId);
        TransactionalEntity<ProjectId, ?, ?> archivedEntity = newEntity(archivedRecordId);

        archive(archivedEntity);

        RecordStorage<ProjectId> storage = storage();
        storage.write(activeRecordId, create(activeRecord, activeEntity));
        storage.write(archivedRecordId, create(archivedRecord, archivedEntity));

        TargetFilters filters = TargetFilters
                .newBuilder()
                .addFilter(all(eq(archived.toString(), true)))
                .build();
        EntityQuery<ProjectId> query = newEntityQuery(filters);
        Iterator<EntityRecord> read = storage.readAll(query);
        assertSingleRecord(archivedRecord, read);
    }

    @Test
    @DisplayName("filter inactive records on bulk read by default")
    void filterWithEmptyQuery() {
        ProjectId activeId = newId();
        ProjectId archivedId = newId();
        ProjectId deletedId = newId();

        TransactionalEntity<ProjectId, ?, ?> activeEntity = newEntity(activeId);
        TransactionalEntity<ProjectId, ?, ?> archivedEntity = newEntity(archivedId);
        TransactionalEntity<ProjectId, ?, ?> deletedEntity = newEntity(deletedId);
        archive(archivedEntity);
        delete(deletedEntity);

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.state(),
                                                       activeEntity.lifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.state(),
                                                         archivedEntity.lifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.state(),
                                                        deletedEntity.lifecycleFlags());

        RecordStorage<ProjectId> storage = storage();
        storage.write(deletedId, create(deletedRecord, deletedEntity));
        storage.write(activeId, create(activeRecord, activeEntity));
        storage.write(archivedId, create(archivedRecord, archivedEntity));

        TargetFilters filters = Targets.acceptingOnly(activeId, archivedId, deletedId);
        EntityQuery<ProjectId> query = newEntityQuery(emptyFilters());

        Iterator<EntityRecord> read = storage.readAll(query);
        assertSingleRecord(activeRecord, read);
    }

    @Test
    @DisplayName("filter archived or deleted records on bulk read with query containing IDs")
    void filterWithQueryIdsAndStatusFilter() {
        ProjectId activeId = newId();
        ProjectId archivedId = newId();
        ProjectId deletedId = newId();

        TransactionalEntity<ProjectId, ?, ?> activeEntity = newEntity(activeId);
        TransactionalEntity<ProjectId, ?, ?> archivedEntity = newEntity(archivedId);
        TransactionalEntity<ProjectId, ?, ?> deletedEntity = newEntity(deletedId);
        archive(archivedEntity);
        delete(deletedEntity);

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.state(),
                                                       activeEntity.lifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.state(),
                                                         archivedEntity.lifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.state(),
                                                        deletedEntity.lifecycleFlags());

        RecordStorage<ProjectId> storage = storage();
        storage.write(deletedId, create(deletedRecord, deletedEntity));
        storage.write(activeId, create(activeRecord, activeEntity));
        storage.write(archivedId, create(archivedRecord, archivedEntity));

        TargetFilters filters = Targets.acceptingOnly(activeId, archivedId, deletedId);
        EntityQuery<ProjectId> query = newEntityQuery(filters).withActiveLifecycle(storage);

        Iterator<EntityRecord> read = storage.readAll(query);
        assertSingleRecord(activeRecord, read);
    }

    @Test
    @DisplayName("filter inactive records on bulk read with query containing IDs by default")
    void filterWithQueryIdsFilterByDefault() {
        ProjectId activeId = newId();
        ProjectId archivedId = newId();
        ProjectId deletedId = newId();

        TransactionalEntity<ProjectId, ?, ?> activeEntity = newEntity(activeId);
        TransactionalEntity<ProjectId, ?, ?> archivedEntity = newEntity(archivedId);
        TransactionalEntity<ProjectId, ?, ?> deletedEntity = newEntity(deletedId);
        archive(archivedEntity);
        delete(deletedEntity);

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.state(),
                                                       activeEntity.lifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.state(),
                                                         archivedEntity.lifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.state(),
                                                        deletedEntity.lifecycleFlags());

        RecordStorage<ProjectId> storage = storage();
        storage.write(deletedId, create(deletedRecord, deletedEntity));
        storage.write(activeId, create(activeRecord, activeEntity));
        storage.write(archivedId, create(archivedRecord, archivedEntity));

        TargetFilters filters = Targets.acceptingOnly(activeId, archivedId, deletedId);
        EntityQuery<ProjectId> query = newEntityQuery(filters);

        Iterator<EntityRecord> read = storage.readAll(query);
        assertSingleRecord(activeRecord, read);
    }

    @Test
    @DisplayName("filter inactive records on bulk read by IDs by default")
    void filterByIdByDefaultInBulk() {
        ProjectId activeId = newId();
        ProjectId archivedId = newId();
        ProjectId deletedId = newId();

        TransactionalEntity<ProjectId, ?, ?> activeEntity = newEntity(activeId);
        TransactionalEntity<ProjectId, ?, ?> archivedEntity = newEntity(archivedId);
        TransactionalEntity<ProjectId, ?, ?> deletedEntity = newEntity(deletedId);
        archive(archivedEntity);
        delete(deletedEntity);

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.state(),
                                                       activeEntity.lifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.state(),
                                                         archivedEntity.lifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.state(),
                                                        deletedEntity.lifecycleFlags());

        RecordStorage<ProjectId> storage = storage();
        storage.write(deletedId, create(deletedRecord, deletedEntity));
        storage.write(activeId, create(activeRecord, activeEntity));
        storage.write(archivedId, create(archivedRecord, archivedEntity));

        ImmutableSet<ProjectId> targetIds = ImmutableSet.of(activeId, archivedId, deletedId);
        Iterable<EntityRecord> read =
                () -> storage.readMultiple(targetIds, FieldMask.getDefaultInstance());

        assertSingleValueAndNulls(2, read);
    }

    private static void assertSingleValueAndNulls(int expectedNullCount,
                                                  Iterable<EntityRecord> values) {
        assertThat(values).hasSize(expectedNullCount + 1);
        int nullCount = 0;
        for (EntityRecord record : values) {
            if (record == null) {
                nullCount++;
            }
        }
        assertEquals(expectedNullCount, nullCount, "An unexpected amount of nulls.");
    }

    @Test
    @DisplayName("update entity column values")
    void updateColumnValues() {
        Project.Status initialStatus = DONE;
        @SuppressWarnings("UnnecessaryLocalVariable") // is used for documentation purposes.
                Project.Status statusAfterUpdate = CANCELLED;
        Int32Value initialStatusValue = Int32Value
                .newBuilder()
                .setValue(initialStatus.getNumber())
                .build();
        Filter status = eq("project_status_value", initialStatusValue);
        CompositeFilter aggregatingFilter = CompositeFilter
                .newBuilder()
                .setOperator(ALL)
                .addFilter(status)
                .build();
        TargetFilters filters = TargetFilters
                .newBuilder()
                .addFilter(aggregatingFilter)
                .build();

        RecordStorage<ProjectId> storage = storage();

        EntityQuery<ProjectId> query = newEntityQuery(filters);

        ProjectId id = newId();
        TestCounterEntity entity = newEntity(id);

        entity.assignStatus(initialStatus);

        EntityRecord record = buildStorageRecord(id, newState(id));
        EntityRecordWithColumns recordWithColumns = create(record, entity);

        FieldMask fieldMask = FieldMask.getDefaultInstance();

        ResponseFormat format = ResponseFormat
                .newBuilder()
                .setFieldMask(fieldMask)
                .vBuild();
        // Create the record.
        storage.write(id, recordWithColumns);
        Iterator<EntityRecord> recordsBefore = storage.readAll(query, format);
        assertSingleRecord(record, recordsBefore);

        // Update the entity columns of the record.
        entity.assignStatus(statusAfterUpdate);

        EntityRecordWithColumns updatedRecordWithColumns = create(record, entity);
        storage.write(id, updatedRecordWithColumns);

        Iterator<EntityRecord> recordsAfter = storage.readAll(query, format);
        assertFalse(recordsAfter.hasNext());
    }

    @Test
    @DisplayName("filter records both by columns and IDs")
    void filterByColumnsAndId() {
        ProjectId targetId = newId();
        TestCounterEntity targetEntity = newEntity(targetId);
        TestCounterEntity noMatchEntity = newEntity(newId());
        TestCounterEntity noMatchIdEntity = newEntity(newId());
        TestCounterEntity deletedEntity = newEntity(newId());

        targetEntity.assignStatus(CANCELLED);
        deletedEntity.assignStatus(CANCELLED);

        delete(deletedEntity);

        noMatchIdEntity.assignStatus(CANCELLED);

        noMatchEntity.assignStatus(DONE);

        write(targetEntity);
        write(noMatchEntity);
        write(noMatchIdEntity);
        write(deletedEntity);

        CompositeFilter filter = all(eq("project_status_value", CANCELLED.getNumber()));
        TargetFilters filters =
                Targets.acceptingOnly(targetId)
                       .toBuilder()
                       .addFilter(filter)
                       .build();
        RecordStorage<ProjectId> storage = storage();
        EntityQuery<ProjectId> query = newEntityQuery(filters).withActiveLifecycle(storage);
        Iterator<EntityRecord> read = storage.readAll(query);
        List<EntityRecord> readRecords = newArrayList(read);
        assertEquals(1, readRecords.size());
        EntityRecord readRecord = readRecords.get(0);
        assertEquals(targetEntity.state(), unpack(readRecord.getState()));
        assertEquals(targetId, Identifier.unpack(readRecord.getEntityId()));
    }

    @Test
    @DisplayName("return a list of entity columns")
    void returnColumnList() {
        S storage = storage();
        ImmutableList<Column> columns = storage.columnList();

        int systemColumnCount = LifecycleFlagField.values().length;
        int protoColumnCount = 6;

        int expectedSize = systemColumnCount + protoColumnCount;
        assertThat(columns).hasSize(expectedSize);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection"/* Storing of generated objects and
                                                               checking via #contains(Object). */)
    @Test
    @DisplayName("create unique states for same ID")
    void createUniqueStatesForSameId() {
        int checkCount = 10;
        ProjectId id = newId();
        Set<EntityState> states = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            EntityState newState = newState(id);
            if (states.contains(newState)) {
                fail("RecordStorageTest.newState() should return unique messages.");
            }
        }
    }

    private void write(Entity<ProjectId, ?> entity) {
        RecordStorage<ProjectId> storage = storage();
        EntityRecord record = buildStorageRecord(entity.id(), entity.state(),
                                                 entity.lifecycleFlags());
        storage.write(entity.id(), create(record, entity));
    }
}
