/*
 * Copyright 2022, TeamDev. All rights reserved.
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

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.EntityState;
import io.spine.client.ResponseFormat;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.LifecycleFlags;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.given.RecordStorageTestEnv;
import io.spine.testing.core.given.GivenVersion;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import static io.spine.protobuf.Messages.isDefault;
import static io.spine.server.entity.given.repository.GivenLifecycleFlags.archived;
import static io.spine.testing.Tests.assertMatchesMask;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract base for tests of record storages.
 * 
 * <p>This abstract test should not contain {@linkplain org.junit.jupiter.api.Nested nested tests}
 * because they are not under control of {@code AbstractRecordStorageTest} inheritors. 
 * Such a control is required for overriding or disabling tests due to a lag between read and 
 * write on remote storages, etc.
 *
 * @param <I>
 *         the type of identifiers a storage uses
 * @param <S>
 *         the type of storage under the test
 */
public abstract class AbstractRecordStorageTest<I, S extends RecordStorage<I>>
        extends AbstractStorageTest<I, EntityRecord, RecordReadRequest<I>, S> {

    private static EntityRecord newStorageRecord(EntityState state) {
        Any wrappedState = pack(state);
        EntityRecord record = EntityRecord
                .newBuilder()
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
     * @param id
     *         the ID for the message
     * @return the unique {@code Message}
     */
    protected abstract EntityState newState(I id);

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
        RecordStorage<I> storage = storage();
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
        ResponseFormat format = ResponseFormat
                .newBuilder()
                .setFieldMask(nonEmptyFieldMask)
                .vBuild();
        RecordStorage<?> storage = storage();
        Iterator<?> empty = storage.readAll(format);

        assertNotNull(empty);
        assertFalse(empty.hasNext(), "Iterator is not empty!");
    }

    @Test
    @DisplayName("delete record")
    void deleteRecord() {
        RecordStorage<I> storage = storage();
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
    @DisplayName("fail to write lifecycle flags to non-existing record")
    void notWriteStatusToNonExistent() {
        I id = newId();
        RecordStorage<I> storage = storage();

        assertThrows(IllegalStateException.class,
                     () -> storage.writeLifecycleFlags(id, archived()));
    }

    @Test
    @DisplayName("accept records with empty storage fields")
    void acceptRecordsWithEmptyColumns() {
        I id = newId();
        EntityRecord record = newStorageRecord(id);
        EntityRecordWithColumns recordWithStorageFields =
                EntityRecordWithColumns.of(record, Collections.emptyMap());
        assertFalse(recordWithStorageFields.hasColumns());
        RecordStorage<I> storage = storage();

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

    @Test
    @DisplayName("given field mask, read single record")
    void singleRecord() {
        I id = newId();
        EntityRecord record = newStorageRecord(id);
        RecordStorage<I> storage = storage();
        storage.write(id, record);

        EntityState state = newState(id);
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
    @DisplayName("given field mask, read multiple records")
    void multipleRecords() {
        RecordStorage<I> storage = storage();
        int count = 10;
        List<I> ids = new ArrayList<>();
        Class<? extends EntityState> stateClass = null;

        for (int i = 0; i < count; i++) {
            I id = newId();
            EntityState state = newState(id);
            if (stateClass == null) {
                stateClass = state.getClass();
            }
            EntityRecord record = newStorageRecord(state);
            storage.write(id, record);
            ids.add(id);
        }

        int bulkCount = count / 2;
        FieldMask fieldMask = fromFieldNumbers(stateClass, 2);
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

    @Test
    @DisplayName("given bulk of records, write them for the first time")
    void forTheFirstTime() {
        RecordStorage<I> storage = storage();
        int bulkSize = 5;

        Map<I, EntityRecordWithColumns> initial = new HashMap<>(bulkSize);

        for (int i = 0; i < bulkSize; i++) {
            I id = newId();
            EntityRecord record = newStorageRecord(id);
            initial.put(id, EntityRecordWithColumns.of(record));
        }
        storage.write(initial);

        Iterator<@Nullable EntityRecord> records =
                storage.readMultiple(initial.keySet(), FieldMask.getDefaultInstance());
        Collection<@Nullable EntityRecord> actual = newArrayList(records);

        @SuppressWarnings("ReturnOfNull") // is fine, since we collect @Nullable
        Collection<@Nullable EntityRecord> expected =
                initial.values()
                       .stream()
                       .map(recordWithColumns -> recordWithColumns != null
                                                 ? recordWithColumns.record()
                                                 : null)
                       .collect(toList());
        assertThat(actual).containsExactlyElementsIn(expected);

        close(storage);
    }

    @Test
    @DisplayName("given bulk of records, write them re-writing existing ones")
    void rewritingExisting() {
        int recordCount = 3;
        RecordStorage<I> storage = storage();

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
        Iterator<EntityRecord> firstRevision = storage.readAll(ResponseFormat.getDefaultInstance());
        RecordStorageTestEnv.assertIteratorsEqual(v1Records.values()
                                                           .iterator(), firstRevision);

        storage.write(transformValues(v2Records, RecordStorageTestEnv::withLifecycleColumns));
        Iterator<EntityRecord> secondRevision =
                storage.readAll(ResponseFormat.getDefaultInstance());
        RecordStorageTestEnv.assertIteratorsEqual(v2Records.values()
                                                           .iterator(), secondRevision);
    }

    @Test
    @DisplayName("return lifecycle flags for missing record")
    void forMissingRecord() {
        I id = newId();
        RecordStorage<I> storage = storage();
        Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertFalse(optional.isPresent());
    }

    @Test
    @DisplayName("return lifecycle flags for new record")
    void forNewRecord() {
        I id = newId();
        EntityRecord record = newStorageRecord(id);
        RecordStorage<I> storage = storage();
        storage.write(id, record);

        Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertTrue(optional.isPresent());
        assertEquals(LifecycleFlags.getDefaultInstance(), optional.get());
    }

    @Test
    @DisplayName("return lifecycle flags for a record where they were updated")
    void forUpdatedRecord() {
        I id = newId();
        EntityRecord record = newStorageRecord(id);
        RecordStorage<I> storage = storage();
        storage.write(id, EntityRecordWithColumns.of(record));

        storage.writeLifecycleFlags(id, archived());

        Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertTrue(optional.isPresent());
        assertTrue(optional.get()
                           .getArchived());
    }
}
