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

import com.google.common.base.Converter;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.entity.FieldMasks;
import org.spine3.server.entity.LifecycleFlags;
import org.spine3.server.entity.storage.EntityRecordEnvelope;
import org.spine3.test.Tests;

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
import static org.spine3.protobuf.AnyPacker.unpack;
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
       extends AbstractStorageShould<I, EntityRecord, EntityRecordEnvelope, S> {

    protected abstract Message newState(I id);

    @Override
    protected EntityRecordEnvelope newStorageRecord() {
        return new EntityRecordEnvelope(newStorageRecord(newState(newId())));
    }

    private EntityRecord newStorageRecord(I id) {
        return newStorageRecord(newState(id));
    }

    private static EntityRecord newStorageRecord(Message state) {
        final Any wrappedState = AnyPacker.pack(state);
        final EntityRecord record = EntityRecord.newBuilder()
                                                .setState(wrappedState)
                                                .setVersion(Tests.newVersionWithNumber(0))
                                                .build();
        return record;
    }

    @Override
    protected Converter<EntityRecordEnvelope, EntityRecord> getRecordConverter() {
        return new RecordConverter();
    }

    @SuppressWarnings("ConstantConditions")
        // Converter nullability issues and Optional getting
    @Test
    public void write_and_read_record_by_Message_id() {
        final RecordStorage<I> storage = getStorage();
        final I id = newId();
        final EntityRecord expected = newStorageRecord(id);
        storage.write(id, getRecordConverter().reverse()
                                              .convert(expected));

        final EntityRecord actual = storage.read(id).get();

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
        storage.write(id, getRecordConverter().reverse()
                                              .convert(record));

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
            storage.write(id, getRecordConverter().reverse()
                                                  .convert(record));
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
        storage.write(id, getRecordConverter().reverse()
                                              .convert(record));

        // Delete the record.
        assertTrue(storage.delete(id));

        // There's no record with such ID.
        assertFalse(storage.read(id).isPresent());
    }

    @Test
    public void write_record_bulk() {
        final RecordStorage<I> storage = getStorage();
        final int bulkSize = 5;

        final Map<I, EntityRecordEnvelope> initial = new HashMap<>(bulkSize);

        for (int i = 0; i < bulkSize; i++) {
            final I id = newId();
            final EntityRecord record = newStorageRecord(id);
            initial.put(id, new EntityRecordEnvelope(record));
        }
        storage.write(initial);

        final Collection<EntityRecord> actual = newLinkedList(
                storage.readMultiple(initial.keySet())
        );
        final Collection<EntityRecord> expected =
                Collections2.transform(initial.values(),
                                       new Function<EntityRecordEnvelope, EntityRecord>() {
                                           @Override
                                           public EntityRecord apply(@Nullable EntityRecordEnvelope envelope) {
                                               assertNotNull(envelope);
                                               return envelope.getRecord();
                                           }
                                       });

        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected));

        close(storage);
    }

    @Test
    public void rewrite_records_in_bulk() {
        final int recordCount = 3;
        final RecordStorage<I> storage = getStorage();

        final Function<EntityRecord, EntityRecordEnvelope> envelopPacker =
                new Function<EntityRecord, EntityRecordEnvelope>() {
                    @Nullable
                    @Override
                    public EntityRecordEnvelope apply(@Nullable EntityRecord record) {
                        if (record == null) {
                            return null;
                        }
                        return new EntityRecordEnvelope(record);
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

        storage.write(Maps.transformValues(v1Records, envelopPacker));
        final Map<I, EntityRecord> firstRevision = storage.readAll();
        assertMapsEqual(v1Records, firstRevision, "First revision EntityRecord-s");

        storage.write(Maps.transformValues(v2Records, envelopPacker));
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
        storage.write(id, getRecordConverter().reverse()
                                              .convert(record));

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
        storage.write(id, new EntityRecordEnvelope(record));

        storage.writeLifecycleFlags(id, archived());

        final Optional<LifecycleFlags> optional = storage.readLifecycleFlags(id);
        assertTrue(optional.isPresent());
        assertTrue(optional.get().getArchived());
    }

    @Test
    public void accept_record_envelopes_with_empty_storage_fields() {
        final I id = newId();
        final EntityRecord record = newStorageRecord(id);
        final EntityRecordEnvelope recordEnvelope = new EntityRecordEnvelope(record);
        assertFalse(recordEnvelope.hasStorageFiedls());
        final RecordStorage<I> storage = getStorage();

        storage.write(id, recordEnvelope);
        final Optional<EntityRecord> actualRecord = storage.read(id);
        assertTrue(actualRecord.isPresent());
        assertEquals(record, actualRecord.get());
    }

    private static class RecordConverter extends Converter<EntityRecordEnvelope, EntityRecord> {
        @Override
        protected EntityRecord doForward(EntityRecordEnvelope entityRecordEnvelope) {
            return entityRecordEnvelope.getRecord();
        }

        @Override
        protected EntityRecordEnvelope doBackward(EntityRecord entityRecord) {
            return new EntityRecordEnvelope(entityRecord);
        }
    }
}
