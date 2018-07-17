/*
 * Copyright 2018, TeamDev. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
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
import io.spine.core.given.GivenVersion;
import io.spine.protobuf.TypeConverter;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.FieldMasks;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.RecordStorageTestEnv.LifecycleColumns;
import io.spine.server.storage.given.RecordStorageTestEnv.TestCounterEntity;
import io.spine.test.storage.Project;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.base.Identifier.pack;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.given.GivenLifecycleFlags.archived;
import static io.spine.server.entity.storage.EntityQueries.from;
import static io.spine.server.entity.storage.EntityRecordWithColumns.create;
import static io.spine.server.entity.storage.TestEntityRecordWithColumnsFactory.createRecord;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.storage.Project.Status.CANCELLED;
import static io.spine.test.storage.Project.Status.DONE;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.Verify.assertIteratorsEqual;
import static io.spine.testing.Verify.assertSize;
import static io.spine.validate.Validate.isDefault;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("unused") // JUnit nested classes considered unused in abstract class.
public abstract class RecordStorageTest<I, S extends RecordStorage<I>>
        extends AbstractStorageTest<I, EntityRecord, RecordReadRequest<I>, S> {

    private static EntityRecord newStorageRecord(Message state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord.newBuilder()
                                          .setState(wrappedState)
                                          .setVersion(GivenVersion.withNumber(0))
                                          .build();
        return record;
    }

    /**
     * Creates an unique {@code Message} with the specified ID.
     *
     * <p>Two calls for the same ID should return messages, which are not equal.
     *
     * @param id the ID for the message
     * @return the unique {@code Message}
     */
    protected abstract Message newState(I id);

    @Override
    protected EntityRecord newStorageRecord() {
        return newStorageRecord(newState(newId()));
    }

    @Override
    protected RecordReadRequest<I> newReadRequest(I id) {
        return new RecordReadRequest<>(id);
    }

    @Override
    protected Class<? extends TestCounterEntity> getTestEntityClass() {
        return TestCounterEntity.class;
    }

    private EntityRecord newStorageRecord(I id) {
        return newStorageRecord(newState(id));
    }

    private EntityRecord newStorageRecord(I id, Message state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setEntityId(pack(id))
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    @SuppressWarnings("ConstantConditions")
    // Converter nullability issues and Optional getting
    @Test
    @DisplayName("write and read record by Message ID")
    void writeAndReadByMessageId() {
        RecordStorage<I> storage = getStorage();
        I id = newId();
        EntityRecord expected = newStorageRecord(id);
        storage.write(id, expected);

        RecordReadRequest<I> readRequest = newReadRequest(id);
        EntityRecord actual = storage.read(readRequest)
                                     .get();

        assertEquals(expected, actual);
        close(storage);
    }

    @Test
    @DisplayName("retrieve empty iterator if storage is empty")
    void retrieveEmptyIterator() {
        FieldMask nonEmptyFieldMask = FieldMask.newBuilder()
                                               .addPaths("invalid-path")
                                               .build();
        RecordStorage storage = getStorage();
        Iterator empty = storage.readAll(nonEmptyFieldMask);

        assertNotNull(empty);
        assertFalse(empty.hasNext(), "Iterator is not empty!");
    }

    @Nested
    @DisplayName("given field mask, read")
    class ReadWithMask {

        @SuppressWarnings("ConstantConditions") // Converter nullability issues.
        @Test
        @DisplayName("single record")
        void singleRecord() {
            I id = newId();
            EntityRecord record = newStorageRecord(id);
            RecordStorage<I> storage = getStorage();
            storage.write(id, record);

            Descriptors.Descriptor descriptor = newState(id).getDescriptorForType();
            FieldMask idMask = FieldMasks.maskOf(descriptor, 1);

            RecordReadRequest<I> readRequest = new RecordReadRequest<>(id);
            Optional<EntityRecord> optional = storage.read(readRequest, idMask);
            assertTrue(optional.isPresent());
            EntityRecord entityRecord = optional.get();

            Message unpacked = unpack(entityRecord.getState());
            assertFalse(isDefault(unpacked));
        }

        @SuppressWarnings({"MethodWithMultipleLoops", "ConstantConditions"})
        // Converter nullability issues.
        @Test
        @DisplayName("multiple records")
        void multipleRecords() {
            RecordStorage<I> storage = getStorage();
            int count = 10;
            List<I> ids = new LinkedList<>();
            Descriptors.Descriptor typeDescriptor = null;

            for (int i = 0; i < count; i++) {
                I id = newId();
                Message state = newState(id);
                EntityRecord record = newStorageRecord(state);
                storage.write(id, record);
                ids.add(id);

                if (typeDescriptor == null) {
                    typeDescriptor = state.getDescriptorForType();
                }
            }

            int bulkCount = count / 2;
            FieldMask fieldMask = FieldMasks.maskOf(typeDescriptor, 2);
            Iterator<EntityRecord> readRecords = storage.readMultiple(
                    ids.subList(0, bulkCount),
                    fieldMask);
            List<EntityRecord> readList = newArrayList(readRecords);
            assertSize(bulkCount, readList);
            for (EntityRecord record : readList) {
                Message state = unpack(record.getState());
                assertMatchesMask(state, fieldMask);
            }
        }
    }

    @SuppressWarnings("ConstantConditions") // converter nullability issues
    @Test
    @DisplayName("delete record")
    void deleteRecord() {
        RecordStorage<I> storage = getStorage();
        I id = newId();
        EntityRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        // Delete the record.
        assertTrue(storage.delete(id));

        // There's no record with such ID.
        RecordReadRequest<I> readRequest = newReadRequest(id);
        assertFalse(storage.read(readRequest)
                           .isPresent());
    }

    @Test
    @DisplayName("write none storage fields if none are passed")
    void writeNoneFieldsIfNonePassed() {
        RecordStorage<I> storage = spy(getStorage());
        I id = newId();
        Any state = pack(Sample.messageOfType(Project.class));
        EntityRecord record =
                Sample.<EntityRecord, EntityRecord.Builder>builderForType(EntityRecord.class)
                        .setState(state)
                        .build();
        storage.write(id, record);
        verify(storage).write(eq(id), withRecordAndNoFields(record));
    }

    @Nested
    @DisplayName("given bulk of records, write them")
    class WriteBulk {

        @Test
        @DisplayName("for the first time")
        void forTheFirstTime() {
            RecordStorage<I> storage = getStorage();
            int bulkSize = 5;

            Map<I, EntityRecordWithColumns> initial = new HashMap<>(bulkSize);

            for (int i = 0; i < bulkSize; i++) {
                I id = newId();
                EntityRecord record = newStorageRecord(id);
                initial.put(id, EntityRecordWithColumns.of(record));
            }
            storage.write(initial);

            Collection<EntityRecord> actual = newArrayList(
                    storage.readMultiple(initial.keySet())
            );

            Collection<EntityRecord> expected =
                    initial.values()
                           .stream()
                           .map(recordWithColumns -> recordWithColumns != null
                                                     ? recordWithColumns.getRecord()
                                                     : null)
                           .collect(Collectors.toList());

            assertEquals(expected.size(), actual.size());
            assertTrue(actual.containsAll(expected));

            close(storage);
        }

        @Test
        @DisplayName("re-writing existing ones")
        void rewritingExisting() {
            int recordCount = 3;
            RecordStorage<I> storage = getStorage();

            Function<EntityRecord, EntityRecordWithColumns> recordPacker =
                    record -> record != null
                              ? withLifecycleColumns(record)
                              : null;
            Map<I, EntityRecord> v1Records = new HashMap<>(recordCount);
            Map<I, EntityRecord> v2Records = new HashMap<>(recordCount);

            for (int i = 0; i < recordCount; i++) {
                I id = newId();
                EntityRecord record = newStorageRecord(id);

                // Some records are changed and some are not
                EntityRecord alternateRecord = (i % 2 == 0)
                                               ? record
                                               : newStorageRecord(id);
                v1Records.put(id, record);
                v2Records.put(id, alternateRecord);
            }

            storage.write(Maps.transformValues(v1Records, recordPacker));
            Iterator<EntityRecord> firstRevision = storage.readAll();
            assertIteratorsEqual(v1Records.values()
                                          .iterator(), firstRevision);

            storage.write(Maps.transformValues(v2Records, recordPacker));
            Iterator<EntityRecord> secondRevision = storage.readAll();
            assertIteratorsEqual(v2Records.values()
                                          .iterator(), secondRevision);
        }
    }

    @Test
    @DisplayName("fail to write visibility to non-existing record")
    void notWriteStatusToNonExistent() {
        I id = newId();
        RecordStorage<I> storage = getStorage();

        assertThrows(IllegalStateException.class,
                     () -> storage.writeLifecycleFlags(id, archived()));
    }

    @Nested
    @DisplayName("return visibility")
    class ReturnVisibility {

        @Test
        @DisplayName("for missing record")
        void forMissingRecord() {
            I id = newId();
            RecordStorage<I> storage = getStorage();
            Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
            assertFalse(optional.isPresent());
        }

        @SuppressWarnings("ConstantConditions") // Converter nullability issues
        @Test
        @DisplayName("for new record")
        void forNewRecord() {
            I id = newId();
            EntityRecord record = newStorageRecord(id);
            RecordStorage<I> storage = getStorage();
            storage.write(id, record);

            Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
            assertTrue(optional.isPresent());
            assertEquals(LifecycleFlags.getDefaultInstance(), optional.get());
        }

        @SuppressWarnings("OptionalGetWithoutIsPresent") // We verify in assertion.
        @Test
        @DisplayName("for record with updated visibility")
        void forUpdatedRecord() {
            I id = newId();
            EntityRecord record = newStorageRecord(id);
            RecordStorage<I> storage = getStorage();
            storage.write(id, EntityRecordWithColumns.of(record));

            storage.writeLifecycleFlags(id, archived());

            Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
            assertTrue(optional.isPresent());
            assertTrue(optional.get()
                               .getArchived());
        }
    }

    @Test
    @DisplayName("accept records with empty storage fields")
    void acceptRecordsWithEmptyColumns() {
        I id = newId();
        EntityRecord record = newStorageRecord(id);
        EntityRecordWithColumns recordWithStorageFields = EntityRecordWithColumns.of(record);
        assertFalse(recordWithStorageFields.hasColumns());
        RecordStorage<I> storage = getStorage();

        storage.write(id, recordWithStorageFields);
        RecordReadRequest<I> readRequest = newReadRequest(id);
        Optional<EntityRecord> actualRecord = storage.read(readRequest);
        assertTrue(actualRecord.isPresent());
        assertEquals(record, actualRecord.get());
    }

    @Test
    @DisplayName("write record with columns")
    void writeRecordWithColumns() {
        I id = newId();
        EntityRecord record = newStorageRecord(id);
        TestCounterEntity<I> testEntity = new TestCounterEntity<>(id);
        RecordStorage<I> storage = getStorage();
        EntityRecordWithColumns recordWithColumns = create(record, testEntity, storage);
        storage.write(id, recordWithColumns);

        RecordReadRequest<I> readRequest = newReadRequest(id);
        Optional<EntityRecord> readRecord = storage.read(readRequest);
        assertTrue(readRecord.isPresent());
        assertEquals(record, readRecord.get());
    }

    @SuppressWarnings("OverlyLongMethod") // Complex test case (still tests a single operation)
    @Test
    @DisplayName("filter records by columns")
    protected void filterByColumns() {
        Project.Status requiredValue = DONE;
        Int32Value wrappedValue = Int32Value
                .newBuilder()
                .setValue(requiredValue.getNumber())
                .build();
        Version versionValue = Version
                .newBuilder()
                .setNumber(2) // Value of the counter after one columns
                .build();     // scan (incremented 2 times internally)

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

        RecordStorage<I> storage = getStorage();

        EntityQuery<I> query = from(filters, storage);
        I idMatching = newId();
        I idWrong1 = newId();
        I idWrong2 = newId();

        TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        TestCounterEntity<I> wrongEntity1 = new TestCounterEntity<>(idWrong1);
        TestCounterEntity<I> wrongEntity2 = new TestCounterEntity<>(idWrong2);

        // 2 of 3 have required values
        matchingEntity.assignStatus(requiredValue);
        wrongEntity1.assignStatus(requiredValue);
        wrongEntity2.assignStatus(CANCELLED);

        // Change internal Entity state
        wrongEntity1.getCounter();

        // After the mutation above the single matching record is the one under the `idMatching` ID

        EntityRecord fineRecord = newStorageRecord(idMatching, newState(idMatching));
        EntityRecord notFineRecord1 = newStorageRecord(idWrong1, newState(idWrong1));
        EntityRecord notFineRecord2 = newStorageRecord(idWrong2, newState(idWrong2));

        EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1, storage);
        EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2, storage);

        storage.write(idMatching, recordRight);
        storage.write(idWrong1, recordWrong1);
        storage.write(idWrong2, recordWrong2);

        Iterator<EntityRecord> readRecords = storage.readAll(query,
                                                             FieldMask.getDefaultInstance());
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    @DisplayName("filter records by ordinal enum columns")
    protected void filterByOrdinalEnumColumns() {
        final String columnPath = "projectStatusOrdinal";
        checkEnumColumnFilter(columnPath);
    }

    @Test
    @DisplayName("filter records by string enum columns")
    protected void filterByStringEnumColumns() {
        final String columnPath = "projectStatusString";
        checkEnumColumnFilter(columnPath);
    }

    @Test
    @DisplayName("filter records by ID and not use columns")
    protected void filterByIdAndNoColumns() {
        // Create the test data
        I idMatching = newId();
        I idWrong1 = newId();
        I idWrong2 = newId();

        TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        TestCounterEntity<I> wrongEntity1 = new TestCounterEntity<>(idWrong1);
        TestCounterEntity<I> wrongEntity2 = new TestCounterEntity<>(idWrong2);

        EntityRecord fineRecord = newStorageRecord(idMatching, newState(idMatching));
        EntityRecord notFineRecord1 = newStorageRecord(idWrong1, newState(idWrong1));
        EntityRecord notFineRecord2 = newStorageRecord(idWrong2, newState(idWrong2));

        RecordStorage<I> storage = getStorage();

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
        EntityQuery<I> query = from(filters, storage);

        // Perform the query
        Iterator<EntityRecord> readRecords =
                storage.readAll(query, FieldMask.getDefaultInstance());
        // Check results
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    @DisplayName("read archived records if specified")
    protected void readArchivedRecords() {
        I activeRecordId = newId();
        I archivedRecordId = newId();

        EntityRecord activeRecord =
                newStorageRecord(activeRecordId, newState(activeRecordId));
        EntityRecord archivedRecord =
                newStorageRecord(archivedRecordId, newState(archivedRecordId));
        TestCounterEntity<I> activeEntity = new TestCounterEntity<>(activeRecordId);
        TestCounterEntity<I> archivedEntity = new TestCounterEntity<>(archivedRecordId);
        archivedEntity.archive();

        RecordStorage<I> storage = getStorage();
        storage.write(activeRecordId, create(activeRecord, activeEntity, storage));
        storage.write(archivedRecordId, create(archivedRecord, archivedEntity, storage));

        EntityFilters filters = EntityFilters
                .newBuilder()
                .addFilter(all(eq(archived.toString(), true)))
                .build();
        EntityQuery<I> query = from(filters, storage);
        Iterator<EntityRecord> read = storage.readAll(query, FieldMask.getDefaultInstance());
        assertSingleRecord(archivedRecord, read);
    }

    @Test
    @DisplayName("filter archived or deleted records by ID on bulk read")
    protected void filterByIdAndStatusInBulk() {
        I activeId = newId();
        I archivedId = newId();
        I deletedId = newId();

        TestCounterEntity<I> activeEntity = new TestCounterEntity<>(activeId);
        TestCounterEntity<I> archivedEntity = new TestCounterEntity<>(archivedId);
        archivedEntity.archive();
        TestCounterEntity<I> deletedEntity = new TestCounterEntity<>(deletedId);
        deletedEntity.delete();

        EntityRecord activeRecord = newStorageRecord(activeId, activeEntity.getState());
        EntityRecord archivedRecord = newStorageRecord(archivedId, archivedEntity.getState());
        EntityRecord deletedRecord = newStorageRecord(deletedId, deletedEntity.getState());

        RecordStorage<I> storage = getStorage();
        storage.write(deletedId, create(deletedRecord, deletedEntity, storage));
        storage.write(activeId, create(activeRecord, activeEntity, storage));
        storage.write(archivedId, create(archivedRecord, archivedEntity, storage));
        EntityIdFilter idFilter = EntityIdFilter
                .newBuilder()
                .addIds(toEntityId(activeId))
                .addIds(toEntityId(archivedId))
                .addIds(toEntityId(deletedId))
                .build();
        EntityFilters filters = EntityFilters.newBuilder()
                                             .setIdFilter(idFilter)
                                             .build();
        EntityQuery<I> query = from(filters, storage).withLifecycleFlags(storage);
        Iterator<EntityRecord> read = storage.readAll(query, FieldMask.getDefaultInstance());
        assertSingleRecord(activeRecord, read);
    }

    @Test
    @DisplayName("update entity column values")
    protected void updateColumnValues() {
        Project.Status initialStatus = DONE;
        @SuppressWarnings("UnnecessaryLocalVariable") // is used for documentation purposes.
                Project.Status statusAfterUpdate = CANCELLED;
        Int32Value initialStatusValue = Int32Value.newBuilder()
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

        RecordStorage<I> storage = getStorage();

        EntityQuery<I> query = from(filters, storage);

        I id = newId();
        TestCounterEntity<I> entity = new TestCounterEntity<>(id);
        entity.assignStatus(initialStatus);

        EntityRecord record = newStorageRecord(id, newState(id));
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
    protected void filterByColumnsAndId() {
        I targetId = newId();
        TestCounterEntity<I> targetEntity = new TestCounterEntity<>(targetId);
        TestCounterEntity<I> noMatchEntity = new TestCounterEntity<>(newId());
        TestCounterEntity<I> noMatchIdEntity = new TestCounterEntity<>(newId());
        TestCounterEntity<I> deletedEntity = new TestCounterEntity<>(newId());

        targetEntity.assignStatus(CANCELLED);
        deletedEntity.assignStatus(CANCELLED);
        deletedEntity.delete();
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
        CompositeColumnFilter columnFilter =
                all(eq("projectStatusValue", CANCELLED.getNumber()));
        EntityFilters filters = EntityFilters
                .newBuilder()
                .setIdFilter(idFilter)
                .addFilter(columnFilter)
                .build();
        RecordStorage<I> storage = getStorage();
        EntityQuery<I> query = from(filters, storage).withLifecycleFlags(storage);
        Iterator<EntityRecord> read = storage.readAll(query, FieldMask.getDefaultInstance());
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
        I id = newId();
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
        final Project.Status requiredValue = DONE;
        final Project.Status value = Enum.valueOf(Project.Status.class, requiredValue.name());
        final ColumnFilter status = eq(columnPath, value);
        final CompositeColumnFilter aggregatingFilter = CompositeColumnFilter.newBuilder()
                                                                             .setOperator(ALL)
                                                                             .addFilter(status)
                                                                             .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(aggregatingFilter)
                                                   .build();

        final RecordStorage<I> storage = getStorage();

        final EntityQuery<I> query = from(filters, storage);
        final I idMatching = newId();
        final I idWrong = newId();

        final TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        final TestCounterEntity<I> wrongEntity = new TestCounterEntity<>(idWrong);

        matchingEntity.assignStatus(requiredValue);
        wrongEntity.assignStatus(CANCELLED);

        final EntityRecord fineRecord = newStorageRecord(idMatching, newState(idMatching));
        final EntityRecord notFineRecord = newStorageRecord(idWrong, newState(idWrong));

        final EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        final EntityRecordWithColumns recordWrong = create(notFineRecord, wrongEntity, storage);

        storage.write(idMatching, recordRight);
        storage.write(idWrong, recordWrong);

        final Iterator<EntityRecord> readRecords = storage.readAll(query,
                                                                   FieldMask.getDefaultInstance());
        assertSingleRecord(fineRecord, readRecords);
    }

    protected static EntityRecordWithColumns withLifecycleColumns(EntityRecord record) {
        final LifecycleFlags flags = record.getLifecycleFlags();
        final Map<String, MemoizedValue> columns = ImmutableMap.of(
                LifecycleColumns.ARCHIVED.columnName(),
                booleanColumn(LifecycleColumns.ARCHIVED.column(), flags.getArchived()),
                LifecycleColumns.DELETED.columnName(),
                booleanColumn(LifecycleColumns.DELETED.column(), flags.getDeleted())
        );
        final EntityRecordWithColumns result = createRecord(record, columns);
        return result;
    }

    private static MemoizedValue booleanColumn(EntityColumn column, boolean value) {
        final MemoizedValue memoizedValue = mock(MemoizedValue.class);
        when(memoizedValue.getSourceColumn()).thenReturn(column);
        when(memoizedValue.getValue()).thenReturn(value);
        return memoizedValue;
    }

    private static EntityRecordWithColumns withRecordAndNoFields(final EntityRecord record) {
        return argThat(argument -> argument.getRecord()
                                           .equals(record)
                && !argument.hasColumns());
    }

    private static void assertSingleRecord(EntityRecord expected, Iterator<EntityRecord> actual) {
        assertTrue(actual.hasNext());
        final EntityRecord singleRecord = actual.next();
        assertFalse(actual.hasNext());
        assertEquals(expected, singleRecord);
    }

    private EntityId toEntityId(I id) {
        Any packed = Identifier.pack(id);
        EntityId entityId = EntityId.newBuilder()
                                    .setId(packed)
                                    .build();
        return entityId;
    }

    private void write(Entity<I, ?> entity) {
        RecordStorage<I> storage = getStorage();
        EntityRecord record = newStorageRecord(entity.getId(), entity.getState());
        storage.write(entity.getId(), create(record, entity, storage));
    }
}
