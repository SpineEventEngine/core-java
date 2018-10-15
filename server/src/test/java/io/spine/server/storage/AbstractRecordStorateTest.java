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

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.RecordStorageTestEnv;
import io.spine.test.storage.Project;
import io.spine.testdata.Sample;
import io.spine.testing.core.given.GivenVersion;
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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.transformValues;
import static com.google.common.truth.Truth.assertThat;
import static com.google.protobuf.util.FieldMaskUtil.fromFieldNumbers;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.server.entity.given.GivenLifecycleFlags.archived;
import static io.spine.validate.Validate.isDefault;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Abstract base for tests of record storages.
 *
 * @param <I> the type of identifiers a storage uses
 * @param <S> the type of storage under the test
 */
public abstract class AbstractRecordStorateTest<I, S extends RecordStorage<I>>
        extends AbstractStorageTest<I, EntityRecord, RecordReadRequest<I>, S> {

    private static EntityRecord newStorageRecord(Message state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
                .setState(wrappedState)
                .setVersion(GivenVersion.withNumber(0))
                .build();
        return record;
    }

    private static EntityRecordWithColumns withRecordAndNoFields(EntityRecord record) {
        return argThat(
                argument -> argument.getRecord()
                                    .equals(record)
                        && !argument.hasColumns()
        );
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
    protected RecordReadRequest<I> newReadRequest(I id) {
        return new RecordReadRequest<>(id);
    }

    protected EntityRecord newStorageRecord(I id) {
        return newStorageRecord(newState(id));
    }

    @Test
    @DisplayName("write and read record by Message ID")
    void writeAndReadByMessageId() {
        RecordStorage<I> storage = getStorage();
        I id = newId();
        EntityRecord expected = newStorageRecord(id);
        storage.write(id, expected);

        RecordReadRequest<I> readRequest = newReadRequest(id);
        Optional<EntityRecord> optional = storage.read(readRequest);
        assertTrue(optional.isPresent());
        EntityRecord actual = optional.get();

        assertEquals(expected, actual);
        close(storage);
    }

    @Test
    @DisplayName("retrieve empty iterator if storage is empty")
    void retrieveEmptyIterator() {
        FieldMask nonEmptyFieldMask = FieldMask
                .newBuilder()
                .addPaths("invalid-path")
                .build();
        RecordStorage storage = getStorage();
        Iterator empty = storage.readAll(nonEmptyFieldMask);

        assertNotNull(empty);
        assertFalse(empty.hasNext(), "Iterator is not empty!");
    }

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
                Sample.<EntityRecord, EntityRecord.Builder>
                        builderForType(EntityRecord.class)
                        .setState(state)
                        .build();
        storage.write(id, record);
        verify(storage).write(eq(id), withRecordAndNoFields(record));
    }

    @Test
    @DisplayName("fail to write lifecycle flags to non-existing record")
    void notWriteStatusToNonExistent() {
        I id = newId();
        RecordStorage<I> storage = getStorage();

        assertThrows(IllegalStateException.class,
                     () -> storage.writeLifecycleFlags(id, archived()));
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

    @Override
    protected EntityRecord newStorageRecord() {
        return newStorageRecord(newState(newId()));
    }

    @Nested
    @DisplayName("given field mask, read")
    class ReadWithMask {

        @Test
        @DisplayName("single record")
        void singleRecord() {
            I id = newId();
            EntityRecord record = newStorageRecord(id);
            RecordStorage<I> storage = getStorage();
            storage.write(id, record);

            Message state = newState(id);
            Descriptor descriptor = state.getDescriptorForType();
            FieldMask idMask = fromFieldNumbers(state.getClass(), 1);

            RecordReadRequest<I> readRequest = new RecordReadRequest<>(id);
            Optional<EntityRecord> optional = storage.read(readRequest, idMask);
            assertTrue(optional.isPresent());
            EntityRecord entityRecord = optional.get();

            Message unpacked = unpack(entityRecord.getState());
            assertFalse(isDefault(unpacked));
        }

        @SuppressWarnings("MethodWithMultipleLoops")
        @Test
        @DisplayName("multiple records")
        void multipleRecords() {
            RecordStorage<I> storage = getStorage();
            int count = 10;
            List<I> ids = new ArrayList<>();
            Class<? extends Message> messageClass = null;

            for (int i = 0; i < count; i++) {
                I id = newId();
                Message state = newState(id);
                if (messageClass == null) {
                    messageClass = state.getClass();
                }
                EntityRecord record = newStorageRecord(state);
                storage.write(id, record);
                ids.add(id);
            }

            int bulkCount = count / 2;
            FieldMask fieldMask = fromFieldNumbers(messageClass, 2);
            Iterator<EntityRecord> readRecords = storage.readMultiple(
                    ids.subList(0, bulkCount),
                    fieldMask);
            List<EntityRecord> readList = newArrayList(readRecords);
            assertThat(readList).hasSize(bulkCount);
            for (EntityRecord record : readList) {
                Message state = unpack(record.getState());
                assertMatchesMask(state, fieldMask);
            }
        }
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
                           .collect(toList());

            assertEquals(expected.size(), actual.size());
            assertTrue(actual.containsAll(expected));

            close(storage);
        }

        @Test
        @DisplayName("re-writing existing ones")
        void rewritingExisting() {
            int recordCount = 3;
            RecordStorage<I> storage = getStorage();

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

            storage.write(transformValues(v1Records, RecordStorageTestEnv::withLifecycleColumns));
            Iterator<EntityRecord> firstRevision = storage.readAll();
            assertIteratorsEqual(v1Records.values()
                                          .iterator(), firstRevision);

            storage.write(transformValues(v2Records, RecordStorageTestEnv::withLifecycleColumns));
            Iterator<EntityRecord> secondRevision = storage.readAll();
            assertIteratorsEqual(v2Records.values()
                                          .iterator(), secondRevision);
        }

        private <E> void assertIteratorsEqual(Iterator<? extends E> first,
                                              Iterator<? extends E> second) {
            Collection<? extends E> firstCollection = newArrayList(first);
            Collection<? extends E> secondCollection = newArrayList(second);
            assertEquals(firstCollection.size(), secondCollection.size());
            assertThat(firstCollection).containsExactlyElementsIn(secondCollection);
        }
    }

    @Nested
    @DisplayName("return lifecycle flags")
    class ReturnLifecycleFlags {

        @Test
        @DisplayName("for missing record")
        void forMissingRecord() {
            I id = newId();
            RecordStorage<I> storage = getStorage();
            Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
            assertFalse(optional.isPresent());
        }

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

        @Test
        @DisplayName("for a record where they were updated")
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
}
