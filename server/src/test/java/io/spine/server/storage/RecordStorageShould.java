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
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.base.Identifier;
import io.spine.base.Time;
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
import io.spine.server.entity.EntityWithLifecycle;
import io.spine.server.entity.EventPlayingEntity;
import io.spine.server.entity.FieldMasks;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.TestTransaction;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import io.spine.server.entity.storage.EntityQueries;
import io.spine.server.entity.storage.EntityQuery;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.entity.storage.Enumerated;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectVBuilder;
import io.spine.testdata.Sample;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.base.Identifier.pack;
import static io.spine.client.ColumnFilters.all;
import static io.spine.client.ColumnFilters.eq;
import static io.spine.client.CompositeColumnFilter.CompositeOperator.ALL;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.entity.TestTransaction.injectState;
import static io.spine.server.entity.given.GivenLifecycleFlags.archived;
import static io.spine.server.entity.storage.EntityRecordWithColumns.create;
import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.server.entity.storage.TestEntityRecordWithColumnsFactory.createRecord;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.test.Tests.assertMatchesMask;
import static io.spine.test.Verify.assertIteratorsEqual;
import static io.spine.test.Verify.assertSize;
import static io.spine.test.storage.Project.Status.CANCELLED;
import static io.spine.test.storage.Project.Status.DONE;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static io.spine.validate.Validate.isDefault;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorageShould<I, S extends RecordStorage<I>>
        extends AbstractStorageShould<I, EntityRecord, RecordReadRequest<I>, S> {

    private static final Function<EntityRecordWithColumns, EntityRecord> RECORD_EXTRACTOR_FUNCTION =
            new Function<EntityRecordWithColumns, EntityRecord>() {
                @Override
                public EntityRecord apply(
                        @Nullable EntityRecordWithColumns entityRecord) {
                    assertNotNull(entityRecord);
                    return entityRecord.getRecord();
                }
            };

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

    private EntityRecord newStorageRecord(I id) {
        return newStorageRecord(newState(id));
    }

    private static EntityRecord newStorageRecord(Message state) {
        final Any wrappedState = pack(state);
        final EntityRecord record = EntityRecord.newBuilder()
                                                .setState(wrappedState)
                                                .setVersion(GivenVersion.withNumber(0))
                                                .build();
        return record;
    }

    private EntityRecord newStorageRecord(I id, Message state) {
        final Any wrappedState = pack(state);
        final EntityRecord record = EntityRecord.newBuilder()
                                                .setEntityId(pack(id))
                                                .setState(wrappedState)
                                                .setVersion(GivenVersion.withNumber(0))
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

        final RecordReadRequest<I> readRequest = newReadRequest(id);
        final EntityRecord actual = storage.read(readRequest)
                                           .get();

        assertEquals(expected, actual);
        close(storage);
    }

    @Override
    protected Class<? extends TestCounterEntity> getTestEntityClass() {
        return TestCounterEntity.class;
    }

    @Test
    public void retrieve_empty_iterator_if_storage_is_empty() {
        final FieldMask nonEmptyFieldMask = FieldMask.newBuilder()
                                                     .addPaths("invalid-path")
                                                     .build();
        final RecordStorage storage = getStorage();
        final Iterator empty = storage.readAll(nonEmptyFieldMask);

        assertNotNull(empty);
        assertFalse("Iterator is not empty!", empty.hasNext());
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

        final RecordReadRequest<I> readRequest = new RecordReadRequest<>(id);
        final Optional<EntityRecord> optional = storage.read(readRequest, idMask);
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
        final Iterator<EntityRecord> readRecords = storage.readMultiple(
                ids.subList(0, bulkCount),
                fieldMask);
        final List<EntityRecord> readList = newArrayList(readRecords);
        assertSize(bulkCount, readList);
        for (EntityRecord record : readList) {
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
        final RecordReadRequest<I> readRequest = newReadRequest(id);
        assertFalse(storage.read(readRequest)
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

        final Collection<EntityRecord> actual = newArrayList(
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
                        return withLifecycleColumns(record);
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
        final Iterator<EntityRecord> firstRevision = storage.readAll();
        assertIteratorsEqual(v1Records.values()
                                      .iterator(), firstRevision);

        storage.write(Maps.transformValues(v2Records, recordPacker));
        final Iterator<EntityRecord> secondRevision = storage.readAll();
        assertIteratorsEqual(v2Records.values()
                                      .iterator(), secondRevision);
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
        final EntityRecordWithColumns recordWithStorageFields = EntityRecordWithColumns.of(record);
        assertFalse(recordWithStorageFields.hasColumns());
        final RecordStorage<I> storage = getStorage();

        storage.write(id, recordWithStorageFields);
        final RecordReadRequest<I> readRequest = newReadRequest(id);
        final Optional<EntityRecord> actualRecord = storage.read(readRequest);
        assertTrue(actualRecord.isPresent());
        assertEquals(record, actualRecord.get());
    }

    @Test
    public void write_record_with_columns() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final TestCounterEntity<I> testEntity = new TestCounterEntity<>(id);
        final RecordStorage<I> storage = getStorage();
        final EntityRecordWithColumns recordWithColumns = create(record, testEntity, storage);
        storage.write(id, recordWithColumns);

        final RecordReadRequest<I> readRequest = newReadRequest(id);
        final Optional<EntityRecord> readRecord = storage.read(readRequest);
        assertTrue(readRecord.isPresent());
        assertEquals(record, readRecord.get());
    }

    @SuppressWarnings("OverlyLongMethod") // Complex test case (still tests a single operation)
    @Test
    public void filter_records_by_columns() {
        final Project.Status requiredValue = DONE;
        final Int32Value wrappedValue = Int32Value.newBuilder()
                                                  .setValue(requiredValue.getNumber())
                                                  .build();
        final Version versionValue = Version.newBuilder()
                                            .setNumber(2) // Value of the counter after one columns
                                            .build();     // scan (incremented 2 times internally)

        final ColumnFilter status = eq("projectStatusValue", wrappedValue);
        final ColumnFilter version = eq("counterVersion", versionValue);
        final CompositeColumnFilter aggregatingFilter = CompositeColumnFilter.newBuilder()
                                                                             .setOperator(ALL)
                                                                             .addFilter(status)
                                                                             .addFilter(version)
                                                                             .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(aggregatingFilter)
                                                   .build();

        final RecordStorage<I> storage = getStorage();

        final EntityQuery<I> query = EntityQueries.from(filters, storage);
        final I idMatching = newId();
        final I idWrong1 = newId();
        final I idWrong2 = newId();

        final TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        final TestCounterEntity<I> wrongEntity1 = new TestCounterEntity<>(idWrong1);
        final TestCounterEntity<I> wrongEntity2 = new TestCounterEntity<>(idWrong2);

        // 2 of 3 have required values
        matchingEntity.setStatus(requiredValue);
        wrongEntity1.setStatus(requiredValue);
        wrongEntity2.setStatus(CANCELLED);

        // Change internal Entity state
        wrongEntity1.getCounter();

        // After the mutation above the single matching record is the one under the `idMatching` ID

        final EntityRecord fineRecord = newStorageRecord(idMatching, newState(idMatching));
        final EntityRecord notFineRecord1 = newStorageRecord(idWrong1, newState(idWrong1));
        final EntityRecord notFineRecord2 = newStorageRecord(idWrong2, newState(idWrong2));

        final EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        final EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1, storage);
        final EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2, storage);

        storage.write(idMatching, recordRight);
        storage.write(idWrong1, recordWrong1);
        storage.write(idWrong2, recordWrong2);

        final Iterator<EntityRecord> readRecords = storage.readAll(query,
                                                                   FieldMask.getDefaultInstance());
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    public void filter_records_by_ordinal_enum_columns() {
        final String columnPath = "projectStatusOrdinal";
        checkEnumColumnFilter(columnPath);
    }

    @Test
    public void filter_records_by_string_enum_columns() {
        final String columnPath = "projectStatusString";
        checkEnumColumnFilter(columnPath);
    }

    @Test
    public void update_entity_column_values() {
        final Project.Status initialStatus = DONE;
        @SuppressWarnings("UnnecessaryLocalVariable") // is used for documentation purposes.
        final Project.Status statusAfterUpdate = CANCELLED;
        final Int32Value initialStatusValue = Int32Value.newBuilder()
                                                        .setValue(initialStatus.getNumber())
                                                        .build();
        final ColumnFilter status = eq("projectStatusValue", initialStatusValue);
        final CompositeColumnFilter aggregatingFilter = CompositeColumnFilter.newBuilder()
                                                                             .setOperator(ALL)
                                                                             .addFilter(status)
                                                                             .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(aggregatingFilter)
                                                   .build();

        final RecordStorage<I> storage = getStorage();

        final EntityQuery<I> query = EntityQueries.from(filters, storage);

        final I id = newId();
        final TestCounterEntity<I> entity = new TestCounterEntity<>(id);
        entity.setStatus(initialStatus);

        final EntityRecord record = newStorageRecord(id, newState(id));
        final EntityRecordWithColumns recordWithColumns = create(record, entity, storage);

        final FieldMask fieldMask = FieldMask.getDefaultInstance();

        // Create the record.
        storage.write(id, recordWithColumns);
        final Iterator<EntityRecord> recordsBefore = storage.readAll(query, fieldMask);
        assertSingleRecord(record, recordsBefore);

        // Update the entity columns of the record.
        entity.setStatus(statusAfterUpdate);
        final EntityRecordWithColumns updatedRecordWithColumns = create(record, entity, storage);
        storage.write(id, updatedRecordWithColumns);

        final Iterator<EntityRecord> recordsAfter = storage.readAll(query, fieldMask);
        assertFalse(recordsAfter.hasNext());
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

        final RecordStorage<I> storage = getStorage();

        final EntityRecordWithColumns recordRight = create(fineRecord, matchingEntity, storage);
        final EntityRecordWithColumns recordWrong1 = create(notFineRecord1, wrongEntity1, storage);
        final EntityRecordWithColumns recordWrong2 = create(notFineRecord2, wrongEntity2, storage);

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
        final EntityQuery<I> query = EntityQueries.from(filters, storage);

        // Perform the query
        final Iterator<EntityRecord> readRecords = storage.readAll(query,
                                                                   FieldMask.getDefaultInstance());
        // Check results
        assertSingleRecord(fineRecord, readRecords);
    }

    @Test
    public void read_archived_records_if_specified() {
        final I activeRecordId = newId();
        final I archivedRecordId = newId();

        final EntityRecord activeRecord = newStorageRecord(activeRecordId,
                                                           newState(activeRecordId));
        final EntityRecord archivedRecord = newStorageRecord(archivedRecordId,
                                                             newState(archivedRecordId));
        final TestCounterEntity<I> activeEntity = new TestCounterEntity<>(activeRecordId);
        final TestCounterEntity<I> archivedEntity = new TestCounterEntity<>(archivedRecordId);
        archivedEntity.archive();

        final RecordStorage<I> storage = getStorage();
        storage.write(activeRecordId, create(activeRecord, activeEntity, storage));
        storage.write(archivedRecordId, create(archivedRecord, archivedEntity, storage));

        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(all(eq(archived.toString(), true)))
                                                   .build();
        final EntityQuery<I> query = EntityQueries.from(filters, storage);
        final Iterator<EntityRecord> read = storage.readAll(query, FieldMask.getDefaultInstance());
        assertSingleRecord(archivedRecord, read);
    }

    @Test
    public void filter_archived_or_deleted_records_on_by_ID_bulk_read() {
        final I activeId = newId();
        final I archivedId = newId();
        final I deletedId = newId();

        final TestCounterEntity<I> activeEntity = new TestCounterEntity<>(activeId);
        final TestCounterEntity<I> archivedEntity = new TestCounterEntity<>(archivedId);
        archivedEntity.archive();
        final TestCounterEntity<I> deletedEntity = new TestCounterEntity<>(deletedId);
        deletedEntity.delete();

        final EntityRecord activeRecord = newStorageRecord(activeId, activeEntity.getState());
        final EntityRecord archivedRecord = newStorageRecord(archivedId, archivedEntity.getState());
        final EntityRecord deletedRecord = newStorageRecord(deletedId, deletedEntity.getState());

        final RecordStorage<I> storage = getStorage();
        storage.write(deletedId, create(deletedRecord, deletedEntity, storage));
        storage.write(activeId, create(activeRecord, activeEntity, storage));
        storage.write(archivedId, create(archivedRecord, archivedEntity, storage));
        final EntityIdFilter idFilter = EntityIdFilter.newBuilder()
                                                      .addIds(toEntityId(activeId))
                                                      .addIds(toEntityId(archivedId))
                                                      .addIds(toEntityId(deletedId))
                                                      .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .build();
        final EntityQuery<I> query = EntityQueries.<I>from(filters, storage)
                                                  .withLifecycleFlags(storage);
        final Iterator<EntityRecord> read = storage.readAll(query, FieldMask.getDefaultInstance());
        assertSingleRecord(activeRecord, read);
    }

    @Test
    public void read_both_by_columns_and_IDs() {
        final I targetId = newId();
        final TestCounterEntity<I> targetEntity = new TestCounterEntity<>(targetId);
        final TestCounterEntity<I> noMatchEntity = new TestCounterEntity<>(newId());
        final TestCounterEntity<I> noMatchIdEntity = new TestCounterEntity<>(newId());
        final TestCounterEntity<I> deletedEntity = new TestCounterEntity<>(newId());

        targetEntity.setStatus(CANCELLED);
        deletedEntity.setStatus(CANCELLED);
        deletedEntity.delete();
        noMatchIdEntity.setStatus(CANCELLED);
        noMatchEntity.setStatus(DONE);

        write(targetEntity);
        write(noMatchEntity);
        write(noMatchIdEntity);
        write(deletedEntity);

        final EntityIdFilter idFilter = EntityIdFilter.newBuilder()
                                                      .addIds(toEntityId(targetId))
                                                      .build();
        final CompositeColumnFilter columnFilter = all(eq("projectStatusValue",
                                                          CANCELLED.getNumber()));
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .setIdFilter(idFilter)
                                                   .addFilter(columnFilter)
                                                   .build();
        final RecordStorage<I> storage = getStorage();
        final EntityQuery<I> query = EntityQueries.<I>from(filters, storage)
                                                  .withLifecycleFlags(storage);
        final Iterator<EntityRecord> read = storage.readAll(query, FieldMask.getDefaultInstance());
        final List<EntityRecord> readRecords = newArrayList(read);
        assertEquals(1, readRecords.size());
        final EntityRecord readRecord = readRecords.get(0);
        assertEquals(targetEntity.getState(), unpack(readRecord.getState()));
        assertEquals(targetId, Identifier.unpack(readRecord.getEntityId()));
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection"/* Storing of generated objects and
                                                               checking via #contains(Object). */)
    @Test
    public void create_unique_states_for_same_ID() {
        final int checkCount = 10;
        final I id = newId();
        final Set<Message> states = newHashSet();
        for (int i = 0; i < checkCount; i++) {
            final Message newState = newState(id);
            if (states.contains(newState)) {
                fail("RecordStorageShould.newState() should return unique messages.");
            }
        }
    }

    /**
     * A complex test case to check the correct {@link TestCounterEntity} filtering by the
     * enumerated column returning {@link ProjectStatus}.
     */
    private void checkEnumColumnFilter(String columnPath) {
        final Project.Status requiredValue = DONE;
        final ProjectStatus value = Enum.valueOf(ProjectStatus.class, requiredValue.name());
        final ColumnFilter status = eq(columnPath, value);
        final CompositeColumnFilter aggregatingFilter = CompositeColumnFilter.newBuilder()
                                                                             .setOperator(ALL)
                                                                             .addFilter(status)
                                                                             .build();
        final EntityFilters filters = EntityFilters.newBuilder()
                                                   .addFilter(aggregatingFilter)
                                                   .build();

        final RecordStorage<I> storage = getStorage();

        final EntityQuery<I> query = EntityQueries.from(filters, storage);
        final I idMatching = newId();
        final I idWrong = newId();

        final TestCounterEntity<I> matchingEntity = new TestCounterEntity<>(idMatching);
        final TestCounterEntity<I> wrongEntity = new TestCounterEntity<>(idWrong);

        matchingEntity.setStatus(requiredValue);
        wrongEntity.setStatus(CANCELLED);

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
        return argThat(new ArgumentMatcher<EntityRecordWithColumns>() {
            @Override
            public boolean matches(EntityRecordWithColumns argument) {
                return argument.getRecord()
                               .equals(record)
                        && !argument.hasColumns();
            }
        });
    }

    private static void assertSingleRecord(EntityRecord expected, Iterator<EntityRecord> actual) {
        assertTrue(actual.hasNext());
        final EntityRecord singleRecord = actual.next();
        assertFalse(actual.hasNext());
        assertEquals(expected, singleRecord);
    }

    private EntityId toEntityId(I id) {
        final Any packed = Identifier.pack(id);
        final EntityId entityId = EntityId.newBuilder()
                                          .setId(packed)
                                          .build();
        return entityId;
    }

    private void write(Entity<I, ?> entity) {
        final RecordStorage<I> storage = getStorage();
        final EntityRecord record = newStorageRecord(entity.getId(), entity.getState());
        storage.write(entity.getId(), create(record, entity, storage));
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestCounterEntity<I> extends EventPlayingEntity<I,
                                                                        Project,
                                                                        ProjectVBuilder> {

        private int counter = 0;

        protected TestCounterEntity(I id) {
            super(id);
        }

        @Column
        public int getCounter() {
            counter++;
            return counter;
        }

        @Column
        public long getBigCounter() {
            return getCounter();
        }

        @Column
        public boolean isCounterEven() {
            return counter % 2 == 0;
        }

        @Column
        public String getCounterName() {
            return getId().toString();
        }

        @Column(name = "COUNTER_VERSION" /* Custom name for storing
                                            to check that querying is correct. */)
        public Version getCounterVersion() {
            return Version.newBuilder()
                          .setNumber(counter)
                          .build();
        }

        @Column
        public Timestamp getNow() {
            return Time.getCurrentTime();
        }

        @Column
        public Project getCounterState() {
            return getState();
        }

        @Column
        public int getProjectStatusValue() {
            return getState().getStatusValue();
        }

        @Column
        public ProjectStatus getProjectStatusOrdinal() {
            return Enum.valueOf(ProjectStatus.class, getState().getStatus().name());
        }

        @Column
        @Enumerated(STRING)
        public ProjectStatus getProjectStatusString() {
            return Enum.valueOf(ProjectStatus.class, getState().getStatus().name());
        }

        private void setStatus(Project.Status status) {
            final Project newState = Project.newBuilder(getState())
                                            .setStatus(status)
                                            .build();
            injectState(this, newState, getCounterVersion());
        }

        private void archive() {
            TestTransaction.archive(this);
        }

        private void delete() {
            TestTransaction.delete(this);
        }
    }

    /**
     * The {@link TestCounterEntity} {@linkplain Project.Status project status} represented by the
     * {@linkplain Enum Java Enum}.
     *
     * <p>Needed to check filtering by the {@link Enumerated} entity columns.
     */
    enum ProjectStatus {
        UNDEFINED ,
        CREATED,
        STARTED,
        DONE,
        CANCELLED,
        UNRECOGNIZED
    }

    /**
     * Entity columns representing lifecycle flags, {@code archived} and {@code deleted}.
     *
     * <p>These columns are present in each {@link EntityWithLifecycle} entity. For the purpose of
     * tests being as close to the real production environment as possible, these columns are stored
     * with the entity records, even if an actual entity is missing.
     *
     * <p>Note that there are cases, when a {@code RecordStorage} stores entity records with no such
     * columns, e.g. the {@linkplain io.spine.server.event.EEntity event entity}. Thus, do not rely
     * on these columns being present in all the entities by default when implementing
     * a {@code RecordStorage}.
     */
    enum LifecycleColumns {

        ARCHIVED("isArchived"),
        DELETED("isDeleted");

        private final EntityColumn column;

        LifecycleColumns(String getterName) {
            try {
                this.column = EntityColumn.from(
                        EntityWithLifecycle.class.getDeclaredMethod(getterName)
                );
            } catch (NoSuchMethodException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        EntityColumn column() {
            return column;
        }

        String columnName() {
            return column.getStoredName();
        }
    }
}
