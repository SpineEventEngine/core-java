/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.client.ColumnFilter;
import io.spine.client.CompositeColumnFilter;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.core.Version;
import io.spine.protobuf.TypeConverter;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.TransactionalEntity;
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
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.storage.EntityRecordWithColumns.create;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.given.RecordStorageTestEnv.archive;
import static io.spine.server.storage.given.RecordStorageTestEnv.assertSingleRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.buildStorageRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.delete;
import static io.spine.server.storage.given.RecordStorageTestEnv.emptyFilters;
import static io.spine.server.storage.given.RecordStorageTestEnv.newEntity;
import static io.spine.server.storage.given.RecordStorageTestEnv.newEntityQuery;
import static io.spine.server.storage.given.RecordStorageTestEnv.toEntityId;
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
    protected Class<? extends TestCounterEntity> getTestEntityClass() {
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
        RecordStorage<ProjectId> storage = getStorage();
        EntityRecordWithColumns recordWithColumns = create(record, testEntity, storage);
        storage.write(id, recordWithColumns);

        RecordReadRequest<ProjectId> readRequest = newReadRequest(id);
        Optional<EntityRecord> readRecord = storage.read(readRequest);
        assertTrue(readRecord.isPresent());
        assertEquals(record, readRecord.get());
    }

    @SuppressWarnings("OverlyLongMethod") // Complex test case (still tests a single operation)
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
                .build();

        ColumnFilter status = eq("projectStatusValue", wrappedValue);
        ColumnFilter version = eq("counterVersion", versionValue);
        CompositeColumnFilter aggregatingFilter = CompositeColumnFilter
                .newBuilder()
                .setOperator(ALL)
                .addFilter(status)
                .addFilter(version)
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .addFilter(aggregatingFilter)
                .build();

        RecordStorage<ProjectId> storage = getStorage();

        EntityQuery<ProjectId> query = newEntityQuery(filters, storage);
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

        EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1, storage);
        EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2, storage);

        storage.write(idMatching, recordRight);
        storage.write(idWrong1, recordWrong1);
        storage.write(idWrong2, recordWrong2);

        Iterator<EntityRecord> readRecords = storage.readAll(query);
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    @DisplayName("filter records by ordinal enum columns")
    protected void filterByOrdinalEnumColumns() {
        String columnPath = "projectStatusOrdinal";
        checkEnumColumnFilter(columnPath);
    }

    @Test
    @DisplayName("filter records by string enum columns")
    protected void filterByStringEnumColumns() {
        String columnPath = "projectStatusString";
        checkEnumColumnFilter(columnPath);
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

        RecordStorage<ProjectId> storage = getStorage();

        EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1, storage);
        EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2, storage);

        // Fill the storage
        storage.write(idWrong1, recordWrong1);
        storage.write(idMatching, recordRight);
        storage.write(idWrong2, recordWrong2);

        // Prepare the query
        Any matchingIdPacked = TypeConverter.toAny(idMatching);
        EntityId entityId = EntityId
                .newBuilder()
                .setId(matchingIdPacked)
                .build();
        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(entityId)
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .build();
        EntityQuery<ProjectId> query = newEntityQuery(filters, storage);

        // Perform the query
        Iterator<EntityRecord> readRecords = storage.readAll(query);
        // Check results
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    @DisplayName("read archived records if specified")
    protected void readArchivedRecords() {
        ProjectId activeRecordId = newId();
        ProjectId archivedRecordId = newId();

        EntityRecord activeRecord =
                buildStorageRecord(activeRecordId, newState(activeRecordId));
        EntityRecord archivedRecord =
                buildStorageRecord(archivedRecordId, newState(archivedRecordId));
        TransactionalEntity<ProjectId, ?, ?> activeEntity = newEntity(activeRecordId);
        TransactionalEntity<ProjectId, ?, ?> archivedEntity = newEntity(archivedRecordId);

        archive(archivedEntity);

        RecordStorage<ProjectId> storage = getStorage();
        storage.write(activeRecordId, create(activeRecord, activeEntity, storage));
        storage.write(archivedRecordId, create(archivedRecord, archivedEntity, storage));

        EntityFilters filters = EntityFilters
                .newBuilder()
                .addFilter(all(eq(archived.toString(), true)))
                .build();
        EntityQuery<ProjectId> query = newEntityQuery(filters, storage);
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

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.getState(),
                                                       activeEntity.getLifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.getState(),
                                                         archivedEntity.getLifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.getState(),
                                                        deletedEntity.getLifecycleFlags());

        RecordStorage<ProjectId> storage = getStorage();
        storage.write(deletedId, create(deletedRecord, deletedEntity, storage));
        storage.write(activeId, create(activeRecord, activeEntity, storage));
        storage.write(archivedId, create(archivedRecord, archivedEntity, storage));

        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(toEntityId(activeId))
                .addIds(toEntityId(archivedId))
                .addIds(toEntityId(deletedId))
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .build();
        EntityQuery<ProjectId> query = newEntityQuery(emptyFilters(), storage);

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

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.getState(),
                                                       activeEntity.getLifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.getState(),
                                                         archivedEntity.getLifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.getState(),
                                                        deletedEntity.getLifecycleFlags());

        RecordStorage<ProjectId> storage = getStorage();
        storage.write(deletedId, create(deletedRecord, deletedEntity, storage));
        storage.write(activeId, create(activeRecord, activeEntity, storage));
        storage.write(archivedId, create(archivedRecord, archivedEntity, storage));

        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(toEntityId(activeId))
                .addIds(toEntityId(archivedId))
                .addIds(toEntityId(deletedId))
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .build();
        EntityQuery<ProjectId> query = newEntityQuery(filters, storage)
                .withActiveLifecycle(storage);

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

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.getState(),
                                                       activeEntity.getLifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.getState(),
                                                         archivedEntity.getLifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.getState(),
                                                        deletedEntity.getLifecycleFlags());

        RecordStorage<ProjectId> storage = getStorage();
        storage.write(deletedId, create(deletedRecord, deletedEntity, storage));
        storage.write(activeId, create(activeRecord, activeEntity, storage));
        storage.write(archivedId, create(archivedRecord, archivedEntity, storage));

        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(toEntityId(activeId))
                .addIds(toEntityId(archivedId))
                .addIds(toEntityId(deletedId))
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .build();
        EntityQuery<ProjectId> query = newEntityQuery(filters, storage);

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

        EntityRecord activeRecord = buildStorageRecord(activeId, activeEntity.getState(),
                                                       activeEntity.getLifecycleFlags());
        EntityRecord archivedRecord = buildStorageRecord(archivedId, archivedEntity.getState(),
                                                         archivedEntity.getLifecycleFlags());
        EntityRecord deletedRecord = buildStorageRecord(deletedId, deletedEntity.getState(),
                                                        deletedEntity.getLifecycleFlags());

        RecordStorage<ProjectId> storage = getStorage();
        storage.write(deletedId, create(deletedRecord, deletedEntity, storage));
        storage.write(activeId, create(activeRecord, activeEntity, storage));
        storage.write(archivedId, create(archivedRecord, archivedEntity, storage));

        ImmutableSet<ProjectId> targetIds = ImmutableSet.of(activeId, archivedId, deletedId);
        Iterable<EntityRecord> read = () -> storage.readMultiple(targetIds);

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
        ColumnFilter status = eq("projectStatusValue", initialStatusValue);
        CompositeColumnFilter aggregatingFilter = CompositeColumnFilter
                .newBuilder()
                .setOperator(ALL)
                .addFilter(status)
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .addFilter(aggregatingFilter)
                .build();

        RecordStorage<ProjectId> storage = getStorage();

        EntityQuery<ProjectId> query = newEntityQuery(filters, storage);

        ProjectId id = newId();
        TestCounterEntity entity = newEntity(id);

        entity.assignStatus(initialStatus);

        EntityRecord record = buildStorageRecord(id, newState(id));
        EntityRecordWithColumns recordWithColumns = create(record, entity, storage);

        FieldMask fieldMask = FieldMask.getDefaultInstance();

        // Create the record.
        storage.write(id, recordWithColumns);
        Iterator<EntityRecord> recordsBefore = storage.readAll(query, fieldMask);
        assertSingleRecord(record, recordsBefore);

        // Update the entity columns of the record.
        entity.assignStatus(statusAfterUpdate);

        EntityRecordWithColumns updatedRecordWithColumns = create(record, entity, storage);
        storage.write(id, updatedRecordWithColumns);

        Iterator<EntityRecord> recordsAfter = storage.readAll(query, fieldMask);
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

        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(toEntityId(targetId))
                .build();
        CompositeColumnFilter columnFilter = all(eq("projectStatusValue", CANCELLED.getNumber()));
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .addFilter(columnFilter)
                .build();
        RecordStorage<ProjectId> storage = getStorage();
        EntityQuery<ProjectId> query = newEntityQuery(filters, storage)
                .withActiveLifecycle(storage);
        Iterator<EntityRecord> read = storage.readAll(query);
        List<EntityRecord> readRecords = newArrayList(read);
        assertEquals(1, readRecords.size());
        EntityRecord readRecord = readRecords.get(0);
        assertEquals(targetEntity.getState(), unpack(readRecord.getState()));
        assertEquals(targetId, Identifier.unpack(readRecord.getEntityId()));
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection"/* Storing of generated objects and
                                                               checking via #contains(Object). */)
    @Test
    @DisplayName("create unique states for same ID")
    void createUniqueStatesForSameId() {
        int checkCount = 10;
        ProjectId id = newId();
        Set<Message> states = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            Message newState = newState(id);
            if (states.contains(newState)) {
                fail("RecordStorageTest.newState() should return unique messages.");
            }
        }
    }

    /**
     * A complex test case to check the correct {@link TestCounterEntity} filtering by the
     * enumerated column returning {@link Project.Status}.
     */
    private void checkEnumColumnFilter(String columnPath) {
        Project.Status requiredValue = DONE;
        Project.Status value = Enum.valueOf(Project.Status.class, requiredValue.name());
        ColumnFilter status = eq(columnPath, value);
        CompositeColumnFilter aggregatingFilter = CompositeColumnFilter
                .newBuilder()
                .setOperator(ALL)
                .addFilter(status)
                .build();
        EntityFilters filters = EntityFilters
                .newBuilder()
                .addFilter(aggregatingFilter)
                .build();

        RecordStorage<ProjectId> storage = getStorage();

        EntityQuery<ProjectId> query = newEntityQuery(filters, storage);
        ProjectId idMatching = newId();
        ProjectId idWrong = newId();

        TestCounterEntity matchingEntity = newEntity(idMatching);
        TestCounterEntity wrongEntity = newEntity(idWrong);

        matchingEntity.assignStatus(requiredValue);
        wrongEntity.assignStatus(CANCELLED);

        EntityRecord fineRecord = buildStorageRecord(idMatching, newState(idMatching));
        EntityRecord notFineRecord = buildStorageRecord(idWrong, newState(idWrong));

        EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        EntityRecordWithColumns recordWrong = create(notFineRecord, wrongEntity, storage);

        storage.write(idMatching, recordRight);
        storage.write(idWrong, recordWrong);

        Iterator<EntityRecord> readRecords = storage.readAll(query);
        assertSingleRecord(fineRecord, readRecords);
    }

    private void write(Entity<ProjectId, ?> entity) {
        RecordStorage<ProjectId> storage = getStorage();
        EntityRecord record = buildStorageRecord(entity.getId(), entity.getState(),
                                                 entity.getLifecycleFlags());
        storage.write(entity.getId(), create(record, entity, storage));
    }
}
