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

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;
import org.spine3.server.entity.FieldMasks;
import org.spine3.server.entity.status.EntityStatus;

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
        final List<EntityStorageRecord> readList = newLinkedList(readRecords);
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

        // Write the record.
        storage.write(id, record);

        // See it is not archived.
        assertFalse(storage.read(id)
                           .get()
                           .getEntityStatus()
                           .getArchived());

        // Mark archived.
        storage.markArchived(id);

        // See that the record is marked.
        assertTrue(storage.read(id)
                          .get()
                          .getEntityStatus()
                          .getArchived());
    }

    @Test(expected = NullPointerException.class)
    public void fail_to_mark_non_existing_record_archived() {
        final I id = newId();
        final RecordStorage<I> storage = getStorage();

        storage.markArchived(id);
    }

    @Test
    public void mark_record_archived_multiple_times() {
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();

        // Write the record.
        storage.write(id, record);

        // See it is not archived.
        assertFalse(storage.read(id)
                           .get()
                           .getEntityStatus()
                           .getArchived());

        markArchivedAndCheck(storage, id);
        markArchivedAndCheck(storage, id);
        markArchivedAndCheck(storage, id);
    }

    private void markArchivedAndCheck(RecordStorage<I> storage, I id) {
        // Mark archived.
        storage.markArchived(id);

        // See that the record is marked.
        assertTrue(storage.read(id)
                          .get()
                          .getEntityStatus()
                          .getArchived());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") // We get right after we write.
    @Test
    public void mark_record_as_deleted() {
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();

        // Write the record.
        storage.write(id, record);

        // See it is not deleted.
        assertFalse(storage.read(id)
                           .get()
                           .getEntityStatus()
                           .getDeleted());

        // Mark deleted.
        storage.markDeleted(id);

        // See that the record is marked.
        assertTrue(storage.read(id)
                          .get()
                          .getEntityStatus()
                          .getDeleted());

        // Check that another attempt to mark deleted returns `false`.
    }

    @Test(expected = NullPointerException.class)
    public void fail_to_mark_non_existing_record_deleted() {
        final I id = newId();
        final RecordStorage<I> storage = getStorage();

        storage.markDeleted(id);
    }

    @Test
    public void mark_record_deleted_multiple_times() {
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);
        final RecordStorage<I> storage = getStorage();

        // Write the record.
        storage.write(id, record);

        // See it is not archived.
        assertFalse(storage.read(id)
                           .get()
                           .getEntityStatus()
                           .getDeleted());

        markDeletedAndCheck(storage, id);
        markDeletedAndCheck(storage, id);
        markDeletedAndCheck(storage, id);
    }

    private void markDeletedAndCheck(RecordStorage<I> storage, I id) {
        // Mark archived.
        storage.markArchived(id);

        // See that the record is marked.
        assertTrue(storage.read(id)
                          .get()
                          .getEntityStatus()
                          .getArchived());
    }

    @Test
    public void delete_record() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        // Delete the record.
        assertTrue(storage.delete(id));

        // There's no record with such ID.
        assertFalse(storage.read(id).isPresent());
    }

    @Test
    public void mark_deleted_after_archived() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        storage.markArchived(id);

        final EntityStatus archived = storage.read(id)
                                             .get()
                                             .getEntityStatus();
        assertTrue(archived.getArchived());
        assertFalse(archived.getDeleted());

        storage.markDeleted(id);

        final EntityStatus archivedAndDeleted = storage.read(id)
                                                       .get()
                                                       .getEntityStatus();
        assertTrue(archivedAndDeleted.getArchived());
        assertTrue(archivedAndDeleted.getDeleted());
    }

    @Test
    public void mark_archived_after_deleted() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityStorageRecord record = newStorageRecord(id);

        // Write the record.
        storage.write(id, record);

        storage.markDeleted(id);

        final EntityStatus deleted = storage.read(id)
                                             .get()
                                             .getEntityStatus();
        assertTrue(deleted.getDeleted());
        assertFalse(deleted.getArchived());

        storage.markArchived(id);

        final EntityStatus archivedAndDeleted = storage.read(id)
                                                       .get()
                                                       .getEntityStatus();
        assertTrue(archivedAndDeleted.getArchived());
        assertTrue(archivedAndDeleted.getDeleted());
    }

    @Test
    public void write_record_bulk() {
        final RecordStorage<I> storage = getStorage();
        final int bulkSize = 5;

        final Map<I, EntityStorageRecord> expected = new HashMap<>(bulkSize);

        for (int i = 0; i < bulkSize; i++) {
            final I id = newId();
            final EntityStorageRecord record = newStorageRecord(id);
            expected.put(id, record);
        }
        storage.write(expected);

        final Collection<EntityStorageRecord> actual = newLinkedList(storage.readMultiple(expected.keySet()));

        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected.values()));

        close(storage);
    }
}
