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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.query.RecordQuery;
import io.spine.server.ServerEnvironment;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.EntityRecordStorageTestEnv;
import io.spine.server.storage.given.EntityRecordStorageTestEnv.TestCounterEntity;
import io.spine.test.storage.StgProject;
import io.spine.test.storage.StgProjectId;
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
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.entity.storage.EntityRecordColumn.archived;
import static io.spine.server.entity.storage.EntityRecordColumn.deleted;
import static io.spine.server.entity.storage.EntityRecordWithColumns.create;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.TestCounterEntity.PROJECT_VERSION_TIMESTAMP;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.archive;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.assertIteratorsEqual;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.assertQueryHasSingleResult;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.assertSingleRecord;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.buildStorageRecord;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.delete;
import static io.spine.server.storage.given.EntityRecordStorageTestEnv.newEntity;
import static io.spine.server.storage.given.GivenStorageProject.newState;
import static io.spine.test.storage.StgProject.Status.CANCELLED;
import static io.spine.test.storage.StgProject.Status.CANCELLED_VALUE;
import static io.spine.test.storage.StgProject.Status.DONE;
import static io.spine.test.storage.StgProject.Status.DONE_VALUE;
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

    @Override
    protected EntityRecordStorage<StgProjectId, StgProject> newStorage() {
        StorageFactory factory = ServerEnvironment.instance()
                                                  .storageFactory();
        return new EntityRecordStorage<>(factory, TestCounterEntity.class, false);
    }

    @Override
    protected EntityRecord newStorageRecord(StgProjectId id) {
        return EntityRecordStorageTestEnv.newStorageRecord(id, newState(id));
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
        EntityRecordStorage<StgProjectId, ?> storage = storage();
        RecordQuery<StgProjectId, EntityRecord> query =
                storage.queryBuilder()
                       .withMask(nonEmptyFieldMask)
                       .build();
        Iterator<?> empty = storage.readAll(query);

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

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection" /* Storing of generated objects and
                                                                checking via #contains(Object). */)
    @Test
    @DisplayName("create unique states for same ID")
    void createUniqueStatesForSameId() {
        int checkCount = 10;
        StgProjectId id = newId();
        Set<EntityState<StgProjectId>> states = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            EntityState<StgProjectId> newState = newState(id);
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

            EntityState<StgProjectId> state = newState(id);
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
            @SuppressWarnings("rawtypes")
            Class<? extends EntityState> stateClass = null;

            for (int i = 0; i < count; i++) {
                StgProjectId id = newId();
                EntityState<StgProjectId> state = newState(id);
                if (stateClass == null) {
                    stateClass = state.getClass();
                }
                EntityRecord record = EntityRecordStorageTestEnv.newStorageRecord(id, state);
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

            storage.write(create(activeEntity, activeRecord));
            storage.write(create(archivedEntity, archivedRecord));

            StgProject.Query query =
                    StgProject.query()
                                               .where(archived.lifecycle(), true)
                                               .build();
            assertQueryHasSingleResult(query, archivedRecord, storage);
        }
    }

    @Nested
    @DisplayName("filter records")
    class Filtering {

        @Test
        @DisplayName("by columns")
        void byColumns() {
            StgProject.Status done = DONE;
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();

            StgProjectId idMatching = newId();
            StgProjectId idWrong1 = newId();
            StgProjectId idWrong2 = newId();

            TestCounterEntity matchingEntity = newEntity(idMatching);
            TestCounterEntity wrong1 = newEntity(idWrong1);
            TestCounterEntity wrong2 = newEntity(idWrong2);

            // 2 of 3 have required values

            matchingEntity.assignStatus(done);
            wrong1.assignStatus(done);
            wrong2.assignStatus(CANCELLED);

            // Change internal Entity state
            wrong1.assignCounter(1);

            // After the mutation above the single matching record is the one
            // under the `idMatching` ID
            EntityRecord fineRecord = writeRecord(storage, idMatching, matchingEntity);
            writeRecord(storage, idWrong1, wrong1);
            writeRecord(storage, idWrong2, wrong2);

            StgProject.Query query =
                    StgProject.query()
                              .projectStatusValue().is(DONE_VALUE)
                              .projectVersion().is(projectVersion())
                              .build();
            assertQueryHasSingleResult(query, fineRecord, storage);
        }

        @CanIgnoreReturnValue
        private EntityRecord writeRecord(EntityRecordStorage<StgProjectId, StgProject> storage,
                                         StgProjectId id,
                                         TestCounterEntity entity) {
            EntityRecord record = buildStorageRecord(id, newState(id));
            EntityRecordWithColumns<StgProjectId> withCols = create(entity, record);
            storage.write(withCols);
            return record;
        }

        private Version projectVersion() {
            Version versionValue = Version
                    .newBuilder()
                    .setNumber(0)
                    .setTimestamp(PROJECT_VERSION_TIMESTAMP)
                    .build();
            return versionValue;
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

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();

            StgProject.Query query =
                    StgProject.query()
                              .id().is(targetId)
                              .projectStatusValue().is(CANCELLED_VALUE)
                              .where(archived.lifecycle(), false)
                              .where(deleted.lifecycle(), false)
                              .build();

            Iterator<EntityRecord> read = storage.findAll(query);
            List<EntityRecord> readRecords = newArrayList(read);
            assertEquals(1, readRecords.size());
            EntityRecord readRecord = readRecords.get(0);
            assertEquals(targetEntity.state(), unpack(readRecord.getState()));
            assertEquals(targetId, Identifier.unpack(readRecord.getEntityId()));
        }

        @Test
        @DisplayName("by ID and without using the columns")
        void byIdAndNoColumns() {
            StgProjectId matchingId = newId();

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecord matchingRecord = writeRecord(matchingId, storage);
            writeRecord(newId(), storage);
            writeRecord(newId(), storage);

            StgProject.Query query =
                    StgProject.query()
                              .id().is(matchingId)
                              .build();
            assertQueryHasSingleResult(query, matchingRecord, storage);
        }

        @CanIgnoreReturnValue
        private EntityRecord writeRecord(StgProjectId id,
                                         EntityRecordStorage<StgProjectId, StgProject> storage) {
            Entity<StgProjectId, ?> entity = newEntity(id);
            EntityRecord record = buildStorageRecord(id, newState(id));
            EntityRecordWithColumns<StgProjectId> withCols = create(entity, record);
            storage.write(withCols);
            return record;
        }

        @Test
        @DisplayName("excluding inactive records by default")
        void emptyQueryExcludesInactive() {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            StgProjectId activeId = newId();

            EntityRecord activeRecord = writeRecord(activeId, storage);
            writeRecordAndArchive(newId(), storage);
            writeRecordAndDelete(newId(), storage);

            Iterator<EntityRecord> read = storage.readAll();
            assertSingleRecord(activeRecord, read);
        }

        @CanIgnoreReturnValue
        private EntityRecord writeRecordAndDelete(
                StgProjectId deletedId, EntityRecordStorage<StgProjectId, StgProject> storage) {
            TestCounterEntity deletedEntity = newEntity(deletedId);
            delete(deletedEntity);
            EntityRecord deletedRecord = buildStorageRecord(deletedEntity);
            storage.write(create(deletedEntity, deletedRecord));
            return deletedRecord;
        }

        @CanIgnoreReturnValue
        private EntityRecord writeRecordAndArchive
                (StgProjectId archivedId, EntityRecordStorage<StgProjectId, StgProject> storage) {
            TestCounterEntity archivedEntity = newEntity(archivedId);
            archive(archivedEntity);
            EntityRecord archivedRecord = buildStorageRecord(archivedEntity);
            storage.write(create(archivedEntity, archivedRecord));
            return archivedRecord;
        }

        @Test
        @DisplayName("including inactive records when reading " +
                "by a query with explicit `archived` and `deleted` flags set to true")
        void explicitFlags() {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            // Writing the active record.
            writeRecord(newId(), storage);
            EntityRecord expectedArchived = writeRecordAndArchive(newId(), storage);
            EntityRecord expectedDeleted = writeRecordAndDelete(newId(), storage);

            StgProject.Query queryArchived =
                    StgProject.query()
                              .where(archived.lifecycle(), true)
                              .build();
            assertQueryHasSingleResult(queryArchived, expectedArchived, storage);

            StgProject.Query queryDeleted =
                    StgProject.query()
                              .where(deleted.lifecycle(), true)
                              .build();
            assertQueryHasSingleResult(queryDeleted, expectedDeleted, storage);
        }

        @Test
        @DisplayName("including inactive records on bulk read by IDs by default")
        void filterByIdByDefaultInBulk() {
            StgProjectId activeId = newId();
            StgProjectId archivedId = newId();
            StgProjectId deletedId = newId();

            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecord activeRecord = writeRecord(activeId, storage);
            EntityRecord archivedRecord = writeRecordAndArchive(archivedId, storage);
            EntityRecord deletedRecord = writeRecordAndDelete(deletedId, storage);

            StgProject.Query query =
                    StgProject.query()
                              .id().in(activeId, archivedId, deletedId)
                              .build();
            Iterator<EntityRecord> actual = storage.findAll(query);

            assertThat(ImmutableSet.copyOf(actual))
                    .containsExactly(activeRecord, archivedRecord, deletedRecord);
        }

        private void write(Entity<StgProjectId, ?> entity) {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            EntityRecord record = buildStorageRecord(entity.id(), entity.state(),
                                                     entity.lifecycleFlags());
            storage.write(create(entity, record));
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
            EntityRecordWithColumns<StgProjectId> recordWithColumns = create(testEntity, record);
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
                initial.put(id, create(id, record));
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

            storage.writeAll(EntityRecordStorageTestEnv.recordsWithColumnsFrom(v1Records));
            Iterator<EntityRecord> firstRevision = storage.readAll();
            assertIteratorsEqual(v1Records.values()
                                          .iterator(), firstRevision);
            storage.writeAll(EntityRecordStorageTestEnv.recordsWithColumnsFrom(v2Records));
            Iterator<EntityRecord> secondRevision = storage.readAll();
            assertIteratorsEqual(v2Records.values()
                                          .iterator(), secondRevision);
        }

        @Test
        @DisplayName("a record and update its column values")
        void updateColumnValues() {
            EntityRecordStorage<StgProjectId, StgProject> storage = storage();
            StgProjectId id = newId();
            TestCounterEntity entity = newEntity(id);
            EntityRecord record = buildStorageRecord(id, newState(id));

            // Write with `DONE` status at first.
            StgProject.Status initialStatus = DONE;
            record = writeWithStatus(entity, initialStatus, record, storage);
            StgProject.Query query =
                    StgProject.query()
                              .projectStatusValue().is(initialStatus.getNumber())
                              .build();
            Iterator<EntityRecord> recordsBefore = storage.findAll(query);
            assertSingleRecord(record, recordsBefore);

            // Update the column status with `CANCELLED` value.
            StgProject.Status statusAfterUpdate = CANCELLED;
            writeWithStatus(entity, statusAfterUpdate, record, storage);

            Iterator<EntityRecord> recordsAfter = storage.findAll(query);
            assertFalse(recordsAfter.hasNext());
        }

        @CanIgnoreReturnValue
        private EntityRecord
        writeWithStatus(TestCounterEntity entity,
                        StgProject.Status status,
                        EntityRecord record,
                        EntityRecordStorage<StgProjectId, StgProject> storage) {
            entity.assignStatus(status);
            EntityRecordWithColumns<StgProjectId> recordWithColumns = create(entity, record);
            storage.write(recordWithColumns);
            return record;
        }

    }

}
