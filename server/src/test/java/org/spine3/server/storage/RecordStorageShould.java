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

import static org.junit.Assert.assertNotNull;
import static org.spine3.test.Tests.assertMatchesMask;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public abstract class RecordStorageShould<I> extends AbstractStorageShould<I, EntityStorageRecord> {

    protected abstract Message newState(I id);

    @Override
    protected EntityStorageRecord newStorageRecord() {
        return newStorageRecord(newState(newId()));
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

    @Test
    public void retrieve_empty_map_if_storage_is_empty() {
        final RecordStorage storage = (RecordStorage) getStorage();

        final FieldMask nonEmptyFieldMask = FieldMask.newBuilder()
                                                     .addPaths("invalid-path")
                                                     .build();

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
}
