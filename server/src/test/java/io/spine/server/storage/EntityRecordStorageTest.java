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
import io.spine.server.entity.storage.EntityRecordSpec;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.LifecycleColumn;
import io.spine.server.storage.given.EntityRecordStorageTestEnv.TestCounterEntity;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
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
import static io.spine.server.entity.storage.LifecycleColumn.archived;
import static io.spine.server.storage.QueryParameters.activeEntityQueryParams;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.TestCounterEntity.PROJECT_VERSION_TIMESTAMP;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.archive;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.assertIteratorsEqual;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.assertSingleRecord;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.buildStorageRecord;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.delete;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.newEntity;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.withLifecycleColumns;
import static io.spine.server.storage.given.GivenStorageProject.newState;
import static io.spine.test.storage.StgProject.Status.CANCELLED;
import static io.spine.test.storage.StgProject.Status.DONE;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.Tests.nullRef;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the storage which store data as {@link EntityRecord}s.
 */
@DisplayName("`EntityRecordStorage` should")
public class EntityRecordStorageTest
        extends AbstractStorageTest<StgProjectId,
                                    EntityRecord,
                                    EntityRecordStorage<StgProjectId, StgProject>> {

    private static EntityRecord newStorageRecord(StgProjectId id, EntityState state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(Identifier.pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    private static List<EntityRecordWithColumns<StgProjectId>>
    recordsWithColumnsFrom(Map<StgProjectId, EntityRecord> recordMap) {
        return recordMap.entrySet()
                        .stream()
                        .map(entry -> withLifecycleColumns(entry.getKey(), entry.getValue()))
                        .collect(toList());
    }

    @Override
    protected EntityRecordStorage<StgProjectId, StgProject> newStorage() {
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new EntityRecordStorage<>(factory, TestCounterEntity.class, false);
    }

    @Override
    protected EntityRecord newStorageRecord(StgProjectId id) {
        return newStorageRecord(id, newState(id));
    }

    @Override
    protected final StgProjectId newId() {
        return StgProjectId.newBuilder()
                           .setId(newUuid())
                           .build();
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
        EntityRecordStorage<?, ?> storage = storage();
        Iterator<?> empty = storage.readAll(format);

        assertNotNull(empty);
        assertFalse(empty.hasNext(), "Iterator is not empty!");
    }

    @Test
    @DisplayName("delete record")
    void deleteRecord() {
        EntityRecordStorage<StgProjectId, StgProject> storage = storage();
        StgProjectId id = newId();
        EntityRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        // Delete the record.
        assertTrue(storage.delete(id));

        // There's no record with such ID.
        assertFalse(storage.read(id)
                           .isPresent());
    }

    @Test
    @DisplayName("return a list of entity columns")
    void returnColumnList() {
        EntityRecordStorage<StgProjectId, StgProject> storage = storage();
        ImmutableList<Column> columnList = storage.recordSpec()
                                                  .columnList();

        int systemColumnCount = LifecycleColumn.values().length;
        int protoColumnCount = 6;

        int expectedSize = systemColumnCount + protoColumnCount;
        assertThat(columnList).hasSize(expectedSize);
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection" /* Storing of generated objects and
                                                                checking via #contains(Object). */)
    @Test
    @DisplayName("create unique states for same ID")
    void createUniqueStatesForSameId() {
        int checkCount = 10;
        StgProjectId id = newId();
        Set<EntityState> states = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            EntityState newState = newState(id);
            if (states.contains(newState)) {
                fail("RecordStorageTest.newState() should return unique messages.");
            }
        }
    }

    @Nested
    @DisplayName("read")
    class Read {

        @Test
        @DisplayName("single record according to the specific field mask")
        void singleRecord() {
            StgProjectId id = newId();
            EntityRecord record = newStorageRecord(id);
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
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
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            int count = 10;
            List<StgProjectId> ids = new ArrayList<>();
            Class<? extends EntityState> stateClass = null;

            for (int i = 0; i < count; i++) {
                StgProjectId id = newId();
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
            StgProjectId activeRecordId = newId();
            StgProjectId archivedRecordId = newId();

            EntityRecord activeRecord =
                    buildStorageRecord(activeRecordId, newState(activeRecordId));
            EntityRecord archivedRecord =
                    buildStorageRecord(archivedRecordId, newState(archivedRecordId));
            TransactionalEntity<StgProjectId, ?, ?> activeEntity = newEntity(activeRecordId);
            TransactionalEntity<StgProjectId, ?, ?> archivedEntity = newEntity(archivedRecordId);

            archive(archivedEntity);

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            storage.write(create(activeEntity, spec, activeRecord));
            storage.write(create(archivedEntity, spec, archivedRecord));

            TargetFilters filters = TargetFilters
                    .newBuilder()
                    .addFilter(all(eq(archived.name(), true)))
                    .build();
            RecordQuery<StgProjectId> query = RecordQueries.from(filters, spec);
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
            StgProject.Status done = DONE;
            Filter status = projectStatusFilter(done);
            Filter version = projectVersionFilter();
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

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();

            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            RecordQuery<StgProjectId> query = RecordQueries.from(filters, spec);
            StgProjectId idMatching = newId();
            StgProjectId idWrong1 = newId();
            StgProjectId idWrong2 = newId();

            TestCounterEntity matchingEntity = newEntity(idMatching);
            TestCounterEntity wrongEntity1 = newEntity(idWrong1);
            TestCounterEntity wrongEntity2 = newEntity(idWrong2);

            // 2 of 3 have required values

            matchingEntity.assignStatus(done);
            wrongEntity1.assignStatus(done);
            wrongEntity2.assignStatus(CANCELLED);

            // Change internal Entity state
            wrongEntity1.assignCounter(1);

            // After the mutation above the single matching record is the one
            // under the `idMatching` ID

            EntityRecord fineRecord = buildStorageRecord(idMatching, newState(idMatching));
            EntityRecord notFineRecord1 = buildStorageRecord(idWrong1, newState(idWrong1));
            EntityRecord notFineRecord2 = buildStorageRecord(idWrong2, newState(idWrong2));

            EntityRecordWithColumns<StgProjectId> recordRight =
                    create(matchingEntity, spec, fineRecord);
            EntityRecordWithColumns<StgProjectId> recordWrong1 =
                    create(wrongEntity1, spec, notFineRecord1);
            EntityRecordWithColumns<StgProjectId> recordWrong2 =
                    create(wrongEntity2, spec, notFineRecord2);

            storage.write(recordRight);
            storage.write(recordWrong1);
            storage.write(recordWrong2);

            Iterator<EntityRecord> readRecords = storage.readAll(query);
            assertSingleRecord(fineRecord, readRecords);
        }

        private Filter projectVersionFilter() {
            Version versionValue = Version
                    .newBuilder()
                    .setNumber(0)
                    .setTimestamp(PROJECT_VERSION_TIMESTAMP)
                    .build();
            return eq(StgProject.Column.projectVersion(), versionValue);
        }

        private Filter projectStatusFilter(StgProject.Status requiredValue) {
            Int32Value wrappedValue = Int32Value
                    .newBuilder()
                    .setValue(requiredValue.getNumber())
                    .build();

            return eq(StgProject.Column.projectStatusValue(), wrappedValue);
        }

        @Test
        @DisplayName("both by columns and IDs")
        void byColumnsAndId() {
            StgProjectId targetId = newId();
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
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            RecordQuery<StgProjectId> filteringQuery = RecordQueries.from(filters,spec);
            RecordQuery<StgProjectId> query = filteringQuery.append(activeEntityQueryParams(spec));
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
            StgProjectId idMatching = newId();
            StgProjectId idWrong1 = newId();
            StgProjectId idWrong2 = newId();

            Entity<StgProjectId, ?> matchingEntity = newEntity(idMatching);
            Entity<StgProjectId, ?> nonMatchingEntity = newEntity(idWrong1);
            Entity<StgProjectId, ?> nonMatchingEntity2 = newEntity(idWrong2);

            EntityRecord matchingRecord = buildStorageRecord(idMatching, newState(idMatching));
            EntityRecord nonMatching1 = buildStorageRecord(idWrong1, newState(idWrong1));
            EntityRecord nonMatching2 = buildStorageRecord(idWrong2, newState(idWrong2));

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();

            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            EntityRecordWithColumns<StgProjectId> matchingWithCols =
                    create(matchingEntity, spec, matchingRecord);
            EntityRecordWithColumns<StgProjectId> nonMatching1WithCols =
                    create(nonMatchingEntity, spec, nonMatching1);
            EntityRecordWithColumns<StgProjectId> nontMatching2WithCols =
                    create(nonMatchingEntity2, spec, nonMatching2);

            // Fill the storage
            storage.write(nonMatching1WithCols);
            storage.write(matchingWithCols);
            storage.write(nontMatching2WithCols);

            // Prepare the query
            Any entityId = Identifier.pack(idMatching);
            TargetFilters filters = Targets.acceptingOnly(entityId);
            RecordQuery<StgProjectId> query = RecordQueries.from(filters, spec);

            // Perform the query
            Iterator<EntityRecord> readRecords = storage.readAll(query);
            // Check results
            assertSingleRecord(matchingRecord, readRecords);
        }

        @Test
        @DisplayName("to exclude inactive records by default")
        void emptyQueryExcludesInactive() {
            StgProjectId activeId = newId();
            StgProjectId archivedId = newId();
            StgProjectId deletedId = newId();

            TestCounterEntity activeEntity = newEntity(activeId);
            TestCounterEntity archivedEntity = newEntity(archivedId);
            TestCounterEntity deletedEntity = newEntity(deletedId);
            archive(archivedEntity);
            delete(deletedEntity);

            EntityRecord activeRecord = buildStorageRecord(activeEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            storage.write(create(deletedEntity, spec, deletedRecord));
            storage.write(create(activeEntity, spec, activeRecord));
            storage.write(create(archivedEntity, spec, archivedRecord));

            Iterator<EntityRecord> read = storage.readAll();
            assertSingleRecord(activeRecord, read);
        }

        @Test
        @DisplayName("include inactive records when reading by a query")
        void withQueryIdsAndStatusFilter() {
            StgProjectId activeId = newId();
            StgProjectId archivedId = newId();
            StgProjectId deletedId = newId();

            TestCounterEntity activeEntity = newEntity(activeId);
            TestCounterEntity archivedEntity = newEntity(archivedId);
            TestCounterEntity deletedEntity = newEntity(deletedId);
            archive(archivedEntity);
            delete(deletedEntity);

            EntityRecord activeRecord = buildStorageRecord(activeEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            storage.write(create(deletedEntity, spec, deletedRecord));
            storage.write(create(activeEntity, spec, activeRecord));
            storage.write(create(archivedEntity, spec, archivedRecord));

            TargetFilters filters = Targets.acceptingOnly(activeId, archivedId, deletedId);
            RecordQuery<StgProjectId> query = RecordQueries.from(filters, spec);
            Iterator<EntityRecord> result = storage.readAll(query);
            ImmutableSet<EntityRecord> actualRecords = ImmutableSet.copyOf(result);

            assertThat(actualRecords).containsExactly(deletedRecord, activeRecord, archivedRecord);
        }

        @Test
        @DisplayName("exclude inactive records on bulk read by IDs by default")
        void filterByIdByDefaultInBulk() {
            StgProjectId activeId = newId();
            StgProjectId archivedId = newId();
            StgProjectId deletedId = newId();

            TestCounterEntity activeEntity = newEntity(activeId);
            TestCounterEntity archivedEntity = newEntity(archivedId);
            TestCounterEntity deletedEntity = newEntity(deletedId);
            archive(archivedEntity);
            delete(deletedEntity);

            EntityRecord activeRecord = buildStorageRecord(activeEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            storage.write(create(deletedEntity, spec, deletedRecord));
            storage.write(create(activeEntity, spec, activeRecord));
            storage.write(create(archivedEntity, spec, archivedRecord));

            ImmutableSet<StgProjectId> targetIds = ImmutableSet.of(activeId, archivedId, deletedId);
            //TODO:2020-04-01:alex.tymchenko: deal with the inconsistency read(ids) vs. read(query).
            Iterator<EntityRecord> actual = storage.readAll(targetIds);

            assertSingleRecord(activeRecord, actual);
        }

        private void write(Entity<StgProjectId, ?> entity) {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecord record = buildStorageRecord(entity.id(), entity.state(),
                                                     entity.lifecycleFlags());
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            storage.write(create(entity, spec, record));
        }
    }

    @Nested
    @DisplayName("write")
    class Write {

        @Test
        @DisplayName("a record with no custom columns")
        void withoutColumns() {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            StgProjectId id = newId();
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
            StgProjectId id = newId();
            EntityRecord record = newStorageRecord(id);
            Entity<StgProjectId, ?> testEntity = newEntity(id);
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordWithColumns<StgProjectId> recordWithColumns =
                    create(testEntity, storage.recordSpec(), record);
            storage.write(recordWithColumns);

            Optional<EntityRecord> readRecord = storage.read(id);
            assertTrue(readRecord.isPresent());
            assertEquals(record, readRecord.get());
        }

        @Test
        @DisplayName("several records which did not exist in storage")
        void severalNewRecords() {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            int bulkSize = 5;

            Map<StgProjectId, EntityRecordWithColumns<StgProjectId>> initial =
                    new HashMap<>(bulkSize);

            for (int i = 0; i < bulkSize; i++) {
                StgProjectId id = newId();
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
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();

            Map<StgProjectId, EntityRecord> v1Records = new HashMap<>(recordCount);
            Map<StgProjectId, EntityRecord> v2Records = new HashMap<>(recordCount);

            for (int i = 0; i < recordCount; i++) {
                StgProjectId id = newId();
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
            assertIteratorsEqual(v1Records.values()
                                          .iterator(), firstRevision);
            storage.writeAll(recordsWithColumnsFrom(v2Records));
            Iterator<EntityRecord> secondRevision =
                    storage.readAll(ResponseFormat.getDefaultInstance());
            assertIteratorsEqual(v2Records.values()
                                          .iterator(), secondRevision);
        }

        @Test
        @DisplayName("a record and update its column values")
        void updateColumnValues() {
            StgProject.Status initialStatus = DONE;
            @SuppressWarnings("UnnecessaryLocalVariable") // is used for documentation purposes.
                    StgProject.Status statusAfterUpdate = CANCELLED;
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

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecordSpec<StgProjectId> spec = storage.recordSpec();
            RecordQuery<StgProjectId> query = RecordQueries.from(filters, spec);

            StgProjectId id = newId();
            TestCounterEntity entity = newEntity(id);

            entity.assignStatus(initialStatus);

            EntityRecord record = buildStorageRecord(id, newState(id));
            EntityRecordWithColumns<StgProjectId> recordWithColumns = create(entity, spec, record);

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

            EntityRecordWithColumns<StgProjectId> updatedRecordWCols = create(entity, spec, record);
            storage.write(updatedRecordWCols);

            Iterator<EntityRecord> recordsAfter = storage.readAll(query, format);
            assertFalse(recordsAfter.hasNext());
        }
    }
}
