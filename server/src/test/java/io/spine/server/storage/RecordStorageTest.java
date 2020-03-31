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
import io.spine.server.ServerEnvironment;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.EntityColumns;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
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
import static io.spine.server.entity.storage.EntityQueries.messageQueryFrom;
import static io.spine.server.entity.storage.EntityRecordWithColumns.create;
import static io.spine.server.entity.storage.QueryParameters.activeEntityQueryParams;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity.PROJECT_VERSION_TIMESTAMP;
import static io.spine.server.storage.given.RecordStorageTestEnv.archive;
import static io.spine.server.storage.given.RecordStorageTestEnv.assertSingleRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.buildStorageRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.delete;
import static io.spine.server.storage.given.RecordStorageTestEnv.newEntity;
import static io.spine.test.storage.Project.Status.CANCELLED;
import static io.spine.test.storage.Project.Status.DONE;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("`EntityRecordStorage` should")
public class RecordStorageTest
        extends AbstractEntityRecordStorageTest<ProjectId, EntityRecordStorage<ProjectId>> {

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

    @Override
    protected EntityRecordStorage<ProjectId> newStorage(Class<? extends Entity<?, ?>> cls) {
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new EntityRecordStorage<>(factory, TestCounterEntity.class, false);
    }

    @Override
    protected EntityState newState(ProjectId id) {
        String uniqueName = format("record-storage-test-%s-%s", id.getId(), nanoTime());
        Project project = Project
                .newBuilder()
                .setId(id)
                .setStatus(Project.Status.CREATED)
                .setName(uniqueName)
                .addTask(Task.getDefaultInstance())
                .build();
        return project;
    }

    @Test
    @DisplayName("write record with columns")
    void writeRecordWithColumns() {
        ProjectId id = newId();
        EntityRecord record = newStorageRecord(id);
        Entity<ProjectId, ?> testEntity = newEntity(id);
        EntityRecordStorage<ProjectId> storage = storage();
        EntityRecordWithColumns<ProjectId> recordWithColumns =
                create(testEntity, storage.columns(), record);
        storage.write(recordWithColumns);

        Optional<EntityRecord> readRecord = storage.read(id);
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

        EntityRecordStorage<ProjectId> storage = storage();

        MessageQuery<ProjectId> query = messageQueryFrom(filters, storage.columns());
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

        EntityRecordWithColumns<ProjectId> recordRight =
                create(matchingEntity, storage.columns(), fineRecord);
        EntityRecordWithColumns<ProjectId> recordWrong1 =
                create(wrongEntity1, storage.columns(), notFineRecord1);
        EntityRecordWithColumns<ProjectId> recordWrong2 =
                create(wrongEntity2, storage.columns(), notFineRecord2);

        storage.write(recordRight);
        storage.write(recordWrong1);
        storage.write(recordWrong2);

        Iterator<EntityRecord> readRecords = storage.readAll(query);
        assertSingleRecord(fineRecord, readRecords);
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

        EntityRecordStorage<ProjectId> storage = storage();

        EntityRecordWithColumns<ProjectId> recordRight =
                create(matchingEntity, storage.columns(), fineRecord);
        EntityRecordWithColumns<ProjectId> recordWrong1 =
                create(wrongEntity1, storage.columns(), notFineRecord1);
        EntityRecordWithColumns<ProjectId> recordWrong2 =
                create(wrongEntity2, storage.columns(), notFineRecord2);

        // Fill the storage
        storage.write(recordWrong1);
        storage.write(recordRight);
        storage.write(recordWrong2);

        // Prepare the query
        Any entityId = TypeConverter.toAny(idMatching);
        TargetFilters filters = Targets.acceptingOnly(entityId);
        MessageQuery<ProjectId> query = messageQueryFrom(filters, storage.columns());

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

        EntityRecordStorage<ProjectId> storage = storage();
        storage.write(create(activeEntity, storage.columns(), activeRecord));
        storage.write(
                create(archivedEntity, storage.columns(), archivedRecord));

        TargetFilters filters = TargetFilters
                .newBuilder()
                .addFilter(all(eq(archived.toString(), true)))
                .build();
        MessageQuery<ProjectId> query = messageQueryFrom(filters, storage.columns());
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

        EntityRecordStorage<ProjectId> storage = storage();
        storage.write(create(deletedEntity, storage.columns(), deletedRecord));
        storage.write(create(activeEntity, storage.columns(), activeRecord));
        storage.write(create(archivedEntity, storage.columns(), archivedRecord));

        Iterator<EntityRecord> read = storage.readAll();
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

        EntityRecordStorage<ProjectId> storage = storage();
        storage.write(create(deletedEntity, storage.columns(), deletedRecord));
        storage.write(create(activeEntity, storage.columns(), activeRecord));
        storage.write(
                create(archivedEntity, storage.columns(), archivedRecord));

        TargetFilters filters = Targets.acceptingOnly(activeId, archivedId, deletedId);
        MessageQuery<ProjectId> query =
                EntityQueries.<ProjectId>messageQueryFrom(filters, storage.columns())
                        .append(activeEntityQueryParams(storage.columns()));
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

        EntityRecordStorage<ProjectId> storage = storage();
        storage.write(create(deletedEntity, storage.columns(), deletedRecord));
        storage.write(create(activeEntity, storage.columns(), activeRecord));
        storage.write(create(archivedEntity, storage.columns(), archivedRecord));

        ImmutableSet<ProjectId> targetIds = ImmutableSet.of(activeId, archivedId, deletedId);
        Iterable<EntityRecord> actual = () -> storage.readAll(targetIds);

        assertThat(actual).hasSize(  1);
        assertThat(actual.iterator().next()).isEqualTo(activeRecord);
    }

//    private static void assertSingleValueAndNulls(int expectedNullCount,
//                                                  Iterable<EntityRecord> values) {
//        assertThat(values).hasSize(expectedNullCount + 1);
//        int nullCount = 0;
//        for (EntityRecord record : values) {
//            if (record == null) {
//                nullCount++;
//            }
//        }
//        assertEquals(expectedNullCount, nullCount, "An unexpected amount of nulls.");
//    }

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

        EntityRecordStorage<ProjectId> storage = storage();
        MessageQuery<ProjectId> query = messageQueryFrom(filters, storage.columns());

        ProjectId id = newId();
        TestCounterEntity entity = newEntity(id);

        entity.assignStatus(initialStatus);

        EntityRecord record = buildStorageRecord(id, newState(id));
        EntityRecordWithColumns<ProjectId> recordWithColumns =
                create(entity, storage.columns(), record);

        FieldMask fieldMask = FieldMask.getDefaultInstance();

        ResponseFormat format = ResponseFormat
                .newBuilder()
                .setFieldMask(fieldMask)
                .vBuild();
        // Create the record.
        storage.write(recordWithColumns);
        Iterator<EntityRecord> recordsBefore = storage.readAll(query, format);
        assertSingleRecord(record, recordsBefore);

        // Update the entity columns of the record.
        entity.assignStatus(statusAfterUpdate);

        EntityRecordWithColumns<ProjectId> updatedRecordWithColumns =
                create(entity, storage.columns(), record);
        storage.write(updatedRecordWithColumns);

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
        EntityRecordStorage<ProjectId> storage = storage();
        MessageQuery<ProjectId> filteringQuery = messageQueryFrom(filters, storage.columns());
        MessageQuery<ProjectId> query =
                filteringQuery.append(activeEntityQueryParams(storage.columns()));
        Iterator<EntityRecord> read = storage.readAll(query);
        List<EntityRecord> readRecords = newArrayList(read);
        assertEquals(1, readRecords.size());
        EntityRecord readRecord = readRecords.get(0);
        assertEquals(targetEntity.state(), unpack(readRecord.getState()));
        assertEquals(targetId, Identifier.unpack(readRecord.getEntityId()));
    }

    //TODO:2020-03-30:alex.tymchenko: move this test.
    @Test
    @DisplayName("return a list of entity columns")
    void returnColumnList() {
        EntityRecordStorage<ProjectId> storage = storage();
        ImmutableList<Column> columnList = storage.columns()
                                                  .columnList();

        int systemColumnCount = LifecycleFlagField.values().length;
        int protoColumnCount = 6;

        int expectedSize = systemColumnCount + protoColumnCount;
        assertThat(columnList).hasSize(expectedSize);
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
        EntityRecordStorage<ProjectId> storage = storage();
        EntityRecord record = buildStorageRecord(entity.id(), entity.state(),
                                                 entity.lifecycleFlags());
        EntityColumns columns = storage.columns();

        storage.write(create(entity, columns, record));
    }
}
