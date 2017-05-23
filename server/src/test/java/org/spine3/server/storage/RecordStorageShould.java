/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.spine3.base.Version;
import org.spine3.client.AggregatingColumnFilter;
import org.spine3.client.ColumnFilter;
import org.spine3.client.ColumnFilters;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.protobuf.TypeConverter;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.EventPlayingEntity;
import org.spine3.server.entity.FieldMasks;
import org.spine3.server.entity.LifecycleFlags;
import org.spine3.server.entity.storage.EntityQueries;
import org.spine3.server.entity.storage.EntityQuery;
import org.spine3.server.entity.storage.EntityRecordWithColumns;
import org.spine3.test.Tests;
import org.spine3.test.storage.Project;
import org.spine3.test.storage.ProjectValidatingBuilder;
import org.spine3.testdata.Sample;
import org.spine3.time.Time;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newLinkedList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.spine3.base.Identifiers.idToAny;
import static org.spine3.client.AggregatingColumnFilter.AggregatingOperator.ALL;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.AnyPacker.unpack;
import static org.spine3.server.entity.TestTransaction.injectState;
import static org.spine3.server.entity.storage.EntityRecordWithColumns.create;
import static org.spine3.test.Tests.archived;
import static org.spine3.test.Tests.assertMatchesMask;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertMapsEqual;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.validate.Validate.isDefault;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorageShould<I, S extends RecordStorage<I>>
        extends AbstractStorageShould<I, EntityRecord, S> {

    private static final Function<EntityRecordWithColumns, EntityRecord> RECORD_EXTRACTOR_FUNCTION =
            new Function<EntityRecordWithColumns, EntityRecord>() {
                @Override
                public EntityRecord apply(
                        @Nullable EntityRecordWithColumns entityRecord) {
                    assertNotNull(entityRecord);
                    return entityRecord.getRecord();
                }
            };

    protected abstract Message newState(I id);

    @Override
    protected EntityRecord newStorageRecord() {
        return newStorageRecord(newState(newId()));
    }

    private EntityRecord newStorageRecord(I id) {
        return newStorageRecord(newState(id));
    }

    private static EntityRecord newStorageRecord(Message state) {
        final Any wrappedState = pack(state);
        final EntityRecord record = EntityRecord.newBuilder()
                                                .setState(wrappedState)
                                                .setVersion(Tests.newVersionWithNumber(0))
                                                .build();
        return record;
    }

    private EntityRecord newStorageRecord(I id, Message state) {
        final Any wrappedState = pack(state);
        final EntityRecord record = EntityRecord.newBuilder()
                                                .setEntityId(idToAny(id))
                                                .setState(wrappedState)
                                                .setVersion(Tests.newVersionWithNumber(0))
                                                .build();
        return record;
    }

    @SuppressWarnings("ConstantConditions")
    // Converter nullability issues and Optional getting
    @Test
    public void write_and_read_record_by_Message_id() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityRecord expected = newStorageRecord(id);
        storage.write(id, expected);

        final EntityRecord actual = storage.read(id)
                                           .get();

        assertEquals(expected, actual);
        close(storage);
    }

    @Test
    public void retrieve_empty_map_if_storage_is_empty() {
        final FieldMask nonEmptyFieldMask = FieldMask.newBuilder()
                                                     .addPaths("invalid-path")
                                                     .build();
        final RecordStorage storage = getStorage();
        final Map empty = storage.readAll(nonEmptyFieldMask);

        assertNotNull(empty);
        assertEmpty(empty);
    }

    @SuppressWarnings("ConstantConditions") // Converter nullability issues
    @Test
    public void read_single_record_with_mask() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();
        storage.write(id, record);

        final Descriptors.Descriptor descriptor = newState(id).getDescriptorForType();
        final FieldMask idMask = FieldMasks.maskOf(descriptor, 1);

        final Optional<EntityRecord> optional = storage.read(id, idMask);
        assertTrue(optional.isPresent());
        final EntityRecord entityRecord = optional.get();

        final Message unpacked = unpack(entityRecord.getState());
        assertFalse(isDefault(unpacked));
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "ConstantConditions"})
    // Converter nullability issues
    @Test
    public void read_multiple_records_with_field_mask() {
        final RecordStorage<I> storage = getStorage();
        final int count = 10;
        final List<I> ids = new LinkedList<>();
        Descriptors.Descriptor typeDescriptor = null;

        for (int i = 0; i < count; i++) {
            final I id = newId();
            final Message state = newState(id);
            final EntityRecord record = newStorageRecord(state);
            storage.write(id, record);
            ids.add(id);

            if (typeDescriptor == null) {
                typeDescriptor = state.getDescriptorForType();
            }
        }

        final int bulkCount = count / 2;
        final FieldMask fieldMask = FieldMasks.maskOf(typeDescriptor, 2);
        final Iterable<EntityRecord> readRecords = storage.readMultiple(
                ids.subList(0, bulkCount),
                fieldMask);
        final List<EntityRecord> readList = newLinkedList(readRecords);
        assertSize(bulkCount, readList);
        for (EntityRecord record : readRecords) {
            final Message state = unpack(record.getState());
            assertMatchesMask(state, fieldMask);
        }
    }

    @SuppressWarnings("ConstantConditions") // converter nullability issues
    @Test
    public void delete_record() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        // Delete the record.
        assertTrue(storage.delete(id));

        // There's no record with such ID.
        assertFalse(storage.read(id)
                           .isPresent());
    }

    @Test
    public void write_none_storage_fields_is_none_passed() {
        final RecordStorage<I> storage = spy(getStorage());
        final I id = newId();
        final Any state = pack(Sample.messageOfType(Project.class));
        final EntityRecord record =
                Sample.<EntityRecord, EntityRecord.Builder>builderForType(EntityRecord.class)
                        .setState(state)
                        .build();
        storage.write(id, record);
        verify(storage).write(eq(id), withRecordAndNoFields(record));
    }

    @Test
    public void write_record_bulk() {
        final RecordStorage<I> storage = getStorage();
        final int bulkSize = 5;

        final Map<I, EntityRecordWithColumns> initial = new HashMap<>(bulkSize);

        for (int i = 0; i < bulkSize; i++) {
            final I id = newId();
            final EntityRecord record = newStorageRecord(id);
            initial.put(id, EntityRecordWithColumns.of(record));
        }
        storage.write(initial);

        final Collection<EntityRecord> actual = newLinkedList(
                storage.readMultiple(initial.keySet())
        );
        final Collection<EntityRecord> expected = Collections2.transform(initial.values(),
                                                                         RECORD_EXTRACTOR_FUNCTION);

        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected));

        close(storage);
    }

    @Test
    public void rewrite_records_in_bulk() {
        final int recordCount = 3;
        final RecordStorage<I> storage = getStorage();

        final Function<EntityRecord, EntityRecordWithColumns> recordPacker =
                new Function<EntityRecord, EntityRecordWithColumns>() {
                    @Nullable
                    @Override
                    public EntityRecordWithColumns apply(@Nullable EntityRecord record) {
                        if (record == null) {
                            return null;
                        }
                        return EntityRecordWithColumns.of(record);
                    }
                };
        final Map<I, EntityRecord> v1Records = new HashMap<>(recordCount);
        final Map<I, EntityRecord> v2Records = new HashMap<>(recordCount);

        for (int i = 0; i < recordCount; i++) {
            final I id = newId();
            final EntityRecord record = newStorageRecord(id);

            // Some records are changed and some are not
            final EntityRecord alternateRecord = (i % 2 == 0)
                    ? record
                    : newStorageRecord(id);
            v1Records.put(id, record);
            v2Records.put(id, alternateRecord);
        }

        storage.write(Maps.transformValues(v1Records, recordPacker));
        final Map<I, EntityRecord> firstRevision = storage.readAll();
        assertMapsEqual(v1Records, firstRevision, "First revision EntityRecord-s");

        storage.write(Maps.transformValues(v2Records, recordPacker));
        final Map<I, EntityRecord> secondRevision = storage.readAll();
        assertMapsEqual(v2Records, secondRevision, "Second revision EntityRecord-s");
    }

    @Test(expected = IllegalStateException.class)
    public void fail_to_write_visibility_to_non_existing_record() {
        final I id = newId();
        final RecordStorage<I> storage = getStorage();

        storage.writeLifecycleFlags(id, archived());
    }

    @Test
    public void return_absent_visibility_for_missing_record() {
        final I id = newId();
        final RecordStorage<I> storage = getStorage();
        final Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertFalse(optional.isPresent());
    }

    @SuppressWarnings("ConstantConditions") // Converter nullability issues
    @Test
    public void return_default_visibility_for_new_record() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();
        storage.write(id, record);

        final Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertTrue(optional.isPresent());
        assertEquals(LifecycleFlags.getDefaultInstance(), optional.get());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We verify in assertion.
    @Test
    public void load_visibility_when_updated() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();
        storage.write(id, EntityRecordWithColumns.of(record));

        storage.writeLifecycleFlags(id, archived());

        final Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertTrue(optional.isPresent());
        assertTrue(optional.get()
                           .getArchived());
    }

    @Test
    public void accept_records_with_empty_storage_fields() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final EntityRecordWithColumns recordWithStorageFields =
                EntityRecordWithColumns.of(record);
        assertFalse(recordWithStorageFields.hasColumns());
        final RecordStorage<I> storage = getStorage();

        storage.write(id, recordWithStorageFields);
        final Optional<EntityRecord> actualRecord = storage.read(id);
        assertTrue(actualRecord.isPresent());
        assertEquals(record, actualRecord.get());
    }

    @Test
    public void write_record_with_columns() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final TestCounterEntity<?> testEntity = new TestCounterEntity<>(id);
        final EntityRecordWithColumns recordWithColumns =
                create(record, testEntity);
        final S storage = getStorage();
        storage.write(id, recordWithColumns);

        final Optional<EntityRecord> readRecord = storage.read(id);
        assertTrue(readRecord.isPresent());
        assertEquals(record, readRecord.get());
    }

    @SuppressWarnings("OverlyLongMethod") // Complex test case (still tests a single operation)
    @Test
    public void filter_records_by_columns() {
        final Project.Status requiredValue = Project.Status.DONE;
        final Int32Value wrappedValue = Int32Value.newBuilder()
                                                  .setValue(requiredValue.getNumber())
                                                  .build();
        final Version versionValue = Version.newBuilder()
                                            .setNumber(2) // Value of the counter after one columns
                                            .build();     // scan (incremented 2 times internally)

        final ColumnFilter status = ColumnFilters.eq("projectStatusValue", wrappedValue);
        final ColumnFilter version = ColumnFilters.eq("counterVersion", versionValue);
        final AggregatingColumnFilter aggregatingFilter = AggregatingColumnFilter.newBuilder()
                                                                                 .setOperator(ALL)
                                                                                 .addFilter(status)
                                                                                 .addFilter(version)
                                                                                 .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(aggregatingFilter)
                                                   .build();
        final EntityQuery<I> query = EntityQueries.from(filters, TestCounterEntity.class);
        final I idMatching = newId();
        final I idWrong1 = newId();
        final I idWrong2 = newId();

        final TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        final TestCounterEntity<I> wrongEntity1 = new TestCounterEntity<>(idWrong1);
        final TestCounterEntity<I> wrongEntity2 = new TestCounterEntity<>(idWrong2);

        // 2 of 3 have required values
        matchingEntity.setStatus(requiredValue);
        wrongEntity1.setStatus(requiredValue);
        wrongEntity2.setStatus(Project.Status.CANCELLED);

        // Change internal Entity state
        wrongEntity1.getCounter();

        // After the mutation above the single matching record is the one under the `idMatching` ID

        final EntityRecord fineRecord = newStorageRecord(idMatching, newState(idMatching));
        final EntityRecord notFineRecord1 = newStorageRecord(idWrong1, newState(idWrong1));
        final EntityRecord notFineRecord2 = newStorageRecord(idWrong2, newState(idWrong2));

        final EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity);
        final EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1);
        final EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2);

        final RecordStorage<I> storage = getStorage();

        storage.write(idMatching, recordRight);
        storage.write(idWrong1, recordWrong1);
        storage.write(idWrong2, recordWrong2);

        final Map<I, EntityRecord> readRecords = storage.readAll(query,
                                                                 FieldMask.getDefaultInstance());
        assertSize(1, readRecords);
        final I singleId = readRecords.keySet()
                                      .iterator()
                                      .next();
        assertEquals(idMatching, singleId);

        final EntityRecord singleRecord = readRecords.values()
                                                     .iterator()
                                                     .next();
        assertEquals(fineRecord, singleRecord);
    }

    @Test
    public void allow_by_single_id_queries_with_no_columns() {
        // Create the test data
        final I idMatching = newId();
        final I idWrong1 = newId();
        final I idWrong2 = newId();

        final TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        final TestCounterEntity<I> wrongEntity1 = new TestCounterEntity<>(idWrong1);
        final TestCounterEntity<I> wrongEntity2 = new TestCounterEntity<>(idWrong2);

        final EntityRecord fineRecord = newStorageRecord(idMatching, newState(idMatching));
        final EntityRecord notFineRecord1 = newStorageRecord(idWrong1, newState(idWrong1));
        final EntityRecord notFineRecord2 = newStorageRecord(idWrong2, newState(idWrong2));

        final EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity);
        final EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1);
        final EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2);

        final RecordStorage<I> storage = getStorage();

        // Fill the storage
        storage.write(idWrong1, recordWrong1);
        storage.write(idMatching, recordRight);
        storage.write(idWrong2, recordWrong2);

        // Prepare the query
        final Any matchingIdPacked = TypeConverter.toAny(idMatching);
        final EntityId entityId = EntityId.newBuilder()
                                          .setId(matchingIdPacked)
                                          .build();
        final EntityIdFilter idFilter = EntityIdFilter.newBuilder()
                                                      .addIds(entityId)
                                                      .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .build();
        final EntityQuery<I> query = EntityQueries.from(filters, TestCounterEntity.class);

        // Perform the query
        final Map<I, EntityRecord> readRecords = storage.readAll(query,
                                                                 FieldMask.getDefaultInstance());
        // Check results
        assertSize(1, readRecords);
        final EntityRecord actualRecord = readRecords.get(idMatching);
        assertEquals(fineRecord, actualRecord);
    }

    private static EntityRecordWithColumns withRecordAndNoFields(final EntityRecord record) {
        return argThat(new ArgumentMatcher<EntityRecordWithColumns>() {
            @Override
            public boolean matches(EntityRecordWithColumns argument) {
                return argument.getRecord()
                               .equals(record)
                        && !argument.hasColumns();
            }
        });
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestCounterEntity<I> extends EventPlayingEntity<I,
                                                                        Project,
                                                                        ProjectValidatingBuilder> {

        private int counter = 0;

        protected TestCounterEntity(I id) {
            super(id);
        }

        public int getCounter() {
            counter++;
            return counter;
        }

        public long getBigCounter() {
            return getCounter();
        }

        public boolean isCounterEven() {
            return counter % 2 == 0;
        }

        public String getCounterName() {
            return getId().toString();
        }

        public Version getCounterVersion() {
            return Version.newBuilder()
                          .setNumber(counter)
                          .build();
        }

        public Timestamp getNow() {
            return Time.getCurrentTime();
        }

        public Project getCounterState() {
            return getState();
        }

        public int getProjectStatusValue() {
            return getState().getStatusValue();
        }

        private void setStatus(Project.Status status) {
            final Project newState = Project.newBuilder(getState())
                                            .setStatus(status)
                                            .build();
            injectState(this, newState, getCounterVersion());
        }
    }
}
