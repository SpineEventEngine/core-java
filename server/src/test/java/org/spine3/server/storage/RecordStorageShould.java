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

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.entity.FieldMasks;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.assertMatchesMask;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorageShould<I, S extends RecordStorage<I>>
       extends AbstractStorageShould<I, EntityStorageRecord, S> {

    protected abstract Message newState(I id);

    @Override
    protected EntityStorageRecord newStorageRecord() {
        return newStorageRecord(newState(newId()));
    }

    private EntityStorageRecord newStorageRecord(I id) {
        return newStorageRecord(newState(id));
    }

    private static EntityStorageRecord newStorageRecord(Message state) {
        final Any wrappedState = AnyPacker.pack(state);
        final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                              .setState(wrappedState)
                                                              .setWhenModified(Timestamps.getCurrentTime())
                                                              .setVersion(0)
                                                              .build();
        return record;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we write.
    @Test
    public void write_and_read_record_by_Message_id() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityStorageRecord expected = newStorageRecord(id);
        storage.write(id, expected);

        final EntityStorageRecord actual = storage.read(id).get();

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

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void read_multiple_records_with_field_mask() {
        final RecordStorage<I> storage = getStorage();
        final int count = 10;
        final List<I> ids = new LinkedList<>();
        Descriptors.Descriptor typeDescriptor = null;

        for (int i = 0; i < count; i++) {
            final I id = newId();
            final Message state = newState(id);
            final EntityStorageRecord record = newStorageRecord(state);
            storage.write(id, record);
            ids.add(id);

            if (typeDescriptor == null) {
                typeDescriptor = state.getDescriptorForType();
            }
        }

        final int bulkCount = count / 2;
        final FieldMask fieldMask = FieldMasks.maskOf(typeDescriptor, 2);
        final Iterable<EntityStorageRecord> readRecords = storage.readMultiple(
                ids.subList(0, bulkCount),
                fieldMask);
        final List<EntityStorageRecord> readList = Lists.newLinkedList(readRecords);
        assertSize(bulkCount, readList);
        for (EntityStorageRecord record : readRecords) {
            final Message state = AnyPacker.unpack(record.getState());
            assertMatchesMask(state, fieldMask);
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we write.
    @Test
    public void mark_record_as_archived() {
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();

        // The attempt to mark a record which is not yet stored returns `false`.
        assertFalse(storage.markArchived(id));

        // Write the record.
        storage.write(id, record);

        // See it is not archived.
        assertFalse(storage.read(id).get().getArchived());

        // Mark archived.
        storage.markArchived(id);

        // See that the record is marked.
        assertTrue(storage.read(id).get().getArchived());

        // Check that another attempt to mark archived returns `false`.
        assertFalse(storage.markArchived(id));
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we write.
    @Test
    public void mark_record_as_deleted() {
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();

        // The attempt to mark a record which is not yet stored returns `false`.
        assertFalse(storage.markDeleted(id));

        // Write the record.
        storage.write(id, record);

        // See it is not deleted.
        assertFalse(storage.read(id).get().getDeleted());

        // Mark deleted.
        storage.markDeleted(id);

        // See that the record is marked.
        assertTrue(storage.read(id).get().getDeleted());

        // Check that another attempt to mark deleted returns `false`.
        assertFalse(storage.markDeleted(id));
    }
}
