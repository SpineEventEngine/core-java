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
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.client.CompositeFilter;
import io.spine.client.Filter;
import io.spine.client.ResponseFormat;
import io.spine.client.TargetFilters;
import io.spine.client.Targets;
import io.spine.core.Version;
import io.spine.server.ServerEnvironment;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.EntityColumns;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.RecordStorageTestEnv;
import io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
import io.spine.testing.core.given.GivenVersion;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.base.Identifier.newUuid;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.Filters.all;
import static io.spine.client.Filters.eq;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.entity.storage.EntityRecordWithColumns.create;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.QueryParameters.activeEntityQueryParams;
import static io.spine.server.storage.RecordQueries.from;
import static io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity.PROJECT_VERSION_TIMESTAMP;
import static io.spine.server.storage.given.RecordStorageTestEnv.archive;
import static io.spine.server.storage.given.RecordStorageTestEnv.assertSingleRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.buildStorageRecord;
import static io.spine.server.storage.given.RecordStorageTestEnv.delete;
import static io.spine.server.storage.given.RecordStorageTestEnv.newEntity;
import static io.spine.server.storage.given.RecordStorageTestEnv.withLifecycleColumns;
import static io.spine.test.storage.Project.Status.CANCELLED;
import static io.spine.test.storage.Project.Status.DONE;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.Tests.nullRef;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Abstract base for tests of message storage implementations.
 *
 * <p>This abstract test should not contain {@linkplain org.junit.jupiter.api.Nested nested tests}
 * because they are not under control of {@code AbstractMessageStorageTest} inheritors.
 * Such a control is required for overriding or disabling tests due to a lag between read and
 * write on remote storage implementations, etc.
 *
 * @param <ProjectId>
 *         the type of identifiers a storage uses
 * @param <S>
 *         the type of storage under the test
 */

@DisplayName("`EntityRecordStorage` should")
public class EntityRecordStorageTest
        extends AbstractStorageTest<ProjectId, EntityRecord, EntityRecordStorage<ProjectId>> {

    @Override
    protected final ProjectId newId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    @Override
    protected Class<? extends Entity<?, ?>> getTestEntityClass() {
        return TestCounterEntity.class;
    }

    @Override
    protected EntityRecordStorage<ProjectId> newStorage(Class<? extends Entity<?, ?>> cls) {
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new EntityRecordStorage<>(factory, TestCounterEntity.class, false);
    }

    @Test
    @DisplayName("retrieve empty iterator if storage is empty")
    void retrieveEmptyIterator() {
        FieldMask nonEmptyFieldMask = FieldMask
                .newBuilder()
                .addPaths("invalid-path")
                .build();
        ResponseFormat format = ResponseFormat
                .newBuilder()
                .setFieldMask(nonEmptyFieldMask)
                .vBuild();
        EntityRecordStorage storage = storage();
        Iterator empty = storage.readAll(format);

        assertNotNull(empty);
        assertFalse(empty.hasNext(), "Iterator is not empty!");
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("single record according to the specific field mask")
        void singleRecord() {
            ProjectId id = newId();
            EntityRecord record = newStorageRecord(id);
            EntityRecordStorage<ProjectId> storage = storage();
            storage.write(id, record);

            EntityState state = newState(id);
            FieldMask idMask = fromFieldNumbers(state.getClass(), 1);

            Optional<EntityRecord> optional = storage.read(id, idMask);
            assertTrue(optional.isPresent());
            EntityRecord entityRecord = optional.get();

            Message unpacked = unpack(entityRecord.getState());
            assertFalse(isDefault(unpacked));
        }

        @SuppressWarnings("MethodWithMultipleLoops")
        @Test
        @DisplayName("multiple records according to the given field mask")
        void multipleRecords() {
            EntityRecordStorage<ProjectId> storage = storage();
            int count = 10;
            List<ProjectId> ids = new ArrayList<>();
            Class<? extends EntityState> stateClass = null;

            for (int i = 0; i < count; i++) {
                ProjectId id = newId();
                EntityState state = newState(id);
                if (stateClass == null) {
                    stateClass = state.getClass();
                }
                EntityRecord record = newStorageRecord(id, state);
                storage.write(id, record);
                ids.add(id);
            }

            int bulkCount = count / 2;
            FieldMask fieldMask = fromFieldNumbers(stateClass, 2);
            Iterator<EntityRecord> readRecords = storage.readAll(ids.subList(0, bulkCount),
                                                                 fieldMask);
            List<EntityRecord> readList = newArrayList(readRecords);
            assertThat(readList).hasSize(bulkCount);
            for (EntityRecord record : readList) {
                Message state = unpack(record.getState());
                assertMatchesMask(state, fieldMask);
            }
        }

        @Test
        @DisplayName("archived records if specified")
        void archivedRecords() {
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
            RecordQuery<ProjectId> query = from(filters, storage.columns());
            Iterator<EntityRecord> read = storage.readAll(query);
            assertSingleRecord(archivedRecord, read);
        }

    }

    @Nested
    @DisplayName("filter records")
    class Filtering {

        @Test
        @DisplayName("by columns")
        void byColumns() {
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

            RecordQuery<ProjectId> query = from(filters, storage.columns());
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
        @DisplayName("both by columns and IDs")
        void byColumnsAndId() {
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
            RecordQuery<ProjectId> filteringQuery = from(filters, storage.columns());
            RecordQuery<ProjectId> query =
                    filteringQuery.append(activeEntityQueryParams(storage.columns()));
            Iterator<EntityRecord> read = storage.readAll(query);
            List<EntityRecord> readRecords = newArrayList(read);
            assertEquals(1, readRecords.size());
            EntityRecord readRecord = readRecords.get(0);
            assertEquals(targetEntity.state(), unpack(readRecord.getState()));
            assertEquals(targetId, Identifier.unpack(readRecord.getEntityId()));
        }

        @Test
        @DisplayName("by ID and not use columns")
        void byIdAndNoColumns() {
            // Create the test data
            ProjectId idMatching = newId();
            ProjectId idWrong1 = newId();
            ProjectId idWrong2 = newId();

            Entity<ProjectId, ?> matchingEntity = newEntity(idMatching);
            Entity<ProjectId, ?> nonMatchingEntity = newEntity(idWrong1);
            Entity<ProjectId, ?> nonMatchingEntity2 = newEntity(idWrong2);

            EntityRecord matchingRecord = buildStorageRecord(idMatching, newState(idMatching));
            EntityRecord nonMatching1 = buildStorageRecord(idWrong1, newState(idWrong1));
            EntityRecord nonMatching2 = buildStorageRecord(idWrong2, newState(idWrong2));

            EntityRecordStorage<ProjectId> storage = storage();

            EntityColumns columns = storage.columns();
            EntityRecordWithColumns<ProjectId> matchingWithCols = create(matchingEntity, columns,
                                                                         matchingRecord);
            EntityRecordWithColumns<ProjectId> nonMatching1WithCols =
                    create(nonMatchingEntity, columns, nonMatching1);
            EntityRecordWithColumns<ProjectId> nontMatching2WithCols =
                    create(nonMatchingEntity2, columns, nonMatching2);

            // Fill the storage
            storage.write(nonMatching1WithCols);
            storage.write(matchingWithCols);
            storage.write(nontMatching2WithCols);

            // Prepare the query
            Any entityId = Identifier.pack(idMatching);
            TargetFilters filters = Targets.acceptingOnly(entityId);
            RecordQuery<ProjectId> query = from(filters, columns);

            // Perform the query
            Iterator<EntityRecord> readRecords = storage.readAll(query);
            // Check results
            assertSingleRecord(matchingRecord, readRecords);
        }

        @Test
        @DisplayName("to exclude inactive records by default")
        void emptyQueryExcludesInactive() {
            ProjectId activeId = newId();
            ProjectId archivedId = newId();
            ProjectId deletedId = newId();

            TestCounterEntity activeEntity = newEntity(activeId);
            TestCounterEntity archivedEntity = newEntity(archivedId);
            TestCounterEntity deletedEntity = newEntity(deletedId);
            archive(archivedEntity);
            delete(deletedEntity);

            EntityRecord activeRecord = buildStorageRecord(activeEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);

            EntityRecordStorage<ProjectId> storage = storage();
            EntityColumns columns = storage.columns();
            storage.write(create(deletedEntity, columns, deletedRecord));
            storage.write(create(activeEntity, columns, activeRecord));
            storage.write(create(archivedEntity, columns, archivedRecord));

            Iterator<EntityRecord> read = storage.readAll();
            assertSingleRecord(activeRecord, read);
        }

        @Test
        @DisplayName("include inactive records when reading by a query")
        void withQueryIdsAndStatusFilter() {
            ProjectId activeId = newId();
            ProjectId archivedId = newId();
            ProjectId deletedId = newId();

            TestCounterEntity activeEntity = newEntity(activeId);
            TestCounterEntity archivedEntity = newEntity(archivedId);
            TestCounterEntity deletedEntity = newEntity(deletedId);
            archive(archivedEntity);
            delete(deletedEntity);

            EntityRecord activeRecord = buildStorageRecord(activeEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);

            EntityRecordStorage<ProjectId> storage = storage();
            EntityColumns columns = storage.columns();
            storage.write(create(deletedEntity, columns, deletedRecord));
            storage.write(create(activeEntity, columns, activeRecord));
            storage.write(create(archivedEntity, columns, archivedRecord));

            TargetFilters filters = Targets.acceptingOnly(activeId, archivedId, deletedId);
            RecordQuery<ProjectId> query = from(filters, columns);
            Iterator<EntityRecord> result = storage.readAll(query);
            ImmutableSet<EntityRecord> actualRecords = ImmutableSet.copyOf(result);

            assertThat(actualRecords).containsExactly(deletedRecord, activeRecord, archivedRecord);
        }

        @Test
        @DisplayName("exclude inactive records on bulk read by IDs by default")
        void filterByIdByDefaultInBulk() {
            ProjectId activeId = newId();
            ProjectId archivedId = newId();
            ProjectId deletedId = newId();

            TestCounterEntity activeEntity = newEntity(activeId);
            TestCounterEntity archivedEntity = newEntity(archivedId);
            TestCounterEntity deletedEntity = newEntity(deletedId);
            archive(archivedEntity);
            delete(deletedEntity);

            EntityRecord activeRecord = buildStorageRecord(activeEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);

            EntityRecordStorage<ProjectId> storage = storage();
            EntityColumns columns = storage.columns();
            storage.write(create(deletedEntity, columns, deletedRecord));
            storage.write(create(activeEntity, columns, activeRecord));
            storage.write(create(archivedEntity, columns, archivedRecord));

            ImmutableSet<ProjectId> targetIds = ImmutableSet.of(activeId, archivedId, deletedId);
            //TODO:2020-04-01:alex.tymchenko: deal with the inconsistency read(ids) vs. read(query).
            Iterator<EntityRecord> actual = storage.readAll(targetIds);

            assertSingleRecord(activeRecord, actual);
        }

        private void write(Entity<ProjectId, ?> entity) {
            EntityRecordStorage<ProjectId> storage = storage();
            EntityRecord record = buildStorageRecord(entity.id(), entity.state(),
                                                     entity.lifecycleFlags());
            EntityColumns columns = storage.columns();

            storage.write(create(entity, columns, record));
        }
    }

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("a record with no custom columns")
        void withoutColumns() {
            EntityRecordStorage<ProjectId> storage = storage();
            ProjectId id = newId();
            EntityRecord expected = newStorageRecord(id);
            storage.write(id, expected);

            Optional<EntityRecord> optional = storage.read(id);
            assertTrue(optional.isPresent());
            EntityRecord actual = optional.get();

            assertEquals(expected, actual);
            close(storage);
        }

        @Test
        @DisplayName("a record with custom columns")
        void withColumns() {
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
        @DisplayName("several records which did not exist in storage")
        void severalNewRecords() {
            EntityRecordStorage<ProjectId> storage = storage();
            int bulkSize = 5;

            Map<ProjectId, EntityRecordWithColumns<ProjectId>> initial = new HashMap<>(bulkSize);

            for (int i = 0; i < bulkSize; i++) {
                ProjectId id = newId();
                EntityRecord record = newStorageRecord(id);
                initial.put(id, EntityRecordWithColumns.create(id, record));
            }
            storage.writeAll(initial.values());

            Iterator<@Nullable EntityRecord> records = storage.readAll(initial.keySet());
            Collection<@Nullable EntityRecord> actual = newArrayList(records);

            Collection<@Nullable EntityRecord> expected =
                    initial.values()
                           .stream()
                           .map(recordWithColumns -> recordWithColumns != null
                                                     ? recordWithColumns.record()
                                                     : nullRef())
                           .collect(toList());
            assertThat(actual).containsExactlyElementsIn(expected);

            close(storage);
        }

        @Test
        @DisplayName("several records which previously existed in storage and rewrite them")
        void rewritingExisting() {
            int recordCount = 3;
            EntityRecordStorage<ProjectId> storage = storage();

            Map<ProjectId, EntityRecord> v1Records = new HashMap<>(recordCount);
            Map<ProjectId, EntityRecord> v2Records = new HashMap<>(recordCount);

            for (int i = 0; i < recordCount; i++) {
                ProjectId id = newId();
                EntityRecord record = newStorageRecord(id);

                // Some records are changed and some are not.
                EntityRecord alternateRecord = (i % 2 == 0)
                                               ? record
                                               : newStorageRecord(id);
                v1Records.put(id, record);
                v2Records.put(id, alternateRecord);
            }

            storage.writeAll(recordsWithColumnsFrom(v1Records));
            Iterator<EntityRecord> firstRevision = storage.readAll();
            RecordStorageTestEnv.assertIteratorsEqual(v1Records.values()
                                                               .iterator(), firstRevision);
            storage.writeAll(recordsWithColumnsFrom(v2Records));
            Iterator<EntityRecord> secondRevision =
                    storage.readAll(ResponseFormat.getDefaultInstance());
            RecordStorageTestEnv.assertIteratorsEqual(v2Records.values()
                                                               .iterator(), secondRevision);
        }

        @Test
        @DisplayName("a record and update its column values")
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
            RecordQuery<ProjectId> query = from(filters, storage.columns());

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
    }

    @Test
    @DisplayName("delete record")
    void deleteRecord() {
        EntityRecordStorage<ProjectId> storage = storage();
        ProjectId id = newId();
        EntityRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        // Delete the record.
        assertTrue(storage.delete(id));

        // There's no record with such ID.
        assertFalse(storage.read(id)
                           .isPresent());
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

    @Override
    protected EntityRecord newStorageRecord(ProjectId id) {
        return newStorageRecord(id, newState(id));
    }

    private static EntityRecord newStorageRecord(ProjectId id, EntityState state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(Identifier.pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    /**
     * Creates an unique {@code EntityState} with the specified ID.
     *
     * <p>Two calls for the same ID should return messages, which are not equal.
     *
     * @param id
     *         the ID for the message
     * @return the unique {@code EntityState}
     */
    private static EntityState newState(ProjectId id) {
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

    private static List<EntityRecordWithColumns<ProjectId>>
    recordsWithColumnsFrom(Map<ProjectId, EntityRecord> recordMap) {
        return recordMap.entrySet()
                        .stream()
                        .map(entry -> withLifecycleColumns(entry.getKey(), entry.getValue()))
                        .collect(toList());
    }
}
