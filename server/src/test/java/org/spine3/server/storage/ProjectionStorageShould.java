/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
import com.google.protobuf.FieldMask;
import com.google.protobuf.Timestamp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Durations;
import org.spine3.protobuf.Timestamps;
import org.spine3.test.Tests;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.Task;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.protobuf.util.Timestamps.add;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Timestamps.getCurrentTime;
import static org.spine3.test.Tests.assertMatchesMask;
import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertEmpty;
import static org.spine3.test.Verify.assertSize;
import static org.spine3.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;

/**
 * Projection storage tests.
 *
 * @param <I> the type of IDs of storage records
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class ProjectionStorageShould<I> extends AbstractStorageShould<I, EntityStorageRecord> {

    private ProjectionStorage<I> storage;

    @Before
    public void setUpProjectionStorageTest() {
        storage = getStorage();
    }

    @After
    public void tearDownProjectionStorageTest() {
        close(storage);
    }

    @Override
    protected abstract ProjectionStorage<I> getStorage();

    @Override
    protected EntityStorageRecord newStorageRecord() {
        return newEntityStorageRecord();
    }

    @Test
    public void return_null_if_no_event_time_in_storage() {
        final Timestamp time = storage.readLastHandledEventTime();

        assertNull(time);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void read_all_messages() {
        final int count = 5;
        final List<I> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final I id = newId();
            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(Any.getDefaultInstance())
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .setVersion(1)
                                                                  .build();
            storage.write(id, record);
            ids.add(id);
        }

        final Map<I, EntityStorageRecord> read = storage.readAll();
        assertSize(ids.size(), read);
        for (Map.Entry<I, EntityStorageRecord> record : read.entrySet()) {
            assertContains(record.getKey(), ids);
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void read_all_messages_with_field_mask() {
        final int count = 5;
        final List<I> ids = new LinkedList<>();

        final String projectDescriptor = Project.getDescriptor()
                                                .getFullName();
        @SuppressWarnings("DuplicateStringLiteralInspection")
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addPaths(projectDescriptor + ".id")
                                             .addPaths(projectDescriptor + ".name")
                                             .build();

        for (int i = 0; i < count; i++) {
            final I id = newId();
            final ProjectId stateId = ProjectId.newBuilder()
                                               .setId(id.toString())
                                               .build();
            final Project state = Project.newBuilder()
                                         .setId(stateId)
                                         .setName(String.format("project_%s", i))
                                         .setStatus(Project.Status.CREATED)
                                         .build();
            final Any packedState = AnyPacker.pack(state);

            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(packedState)
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .setVersion(1)
                                                                  .build();
            storage.write(id, record);
            ids.add(id);
        }

        final Map<I, EntityStorageRecord> read = storage.readAll(fieldMask);
        assertSize(ids.size(), read);
        for (Map.Entry<I, EntityStorageRecord> record : read.entrySet()) {
            assertContains(record.getKey(), ids);

            final Any packedState = record.getValue()
                                          .getState();
            final Project state = AnyPacker.unpack(packedState);
            assertMatchesMask(state, fieldMask);
        }
    }

    @Test
    public void retrieve_empty_map_if_store_is_empty() {
        final Map<I, EntityStorageRecord> noMask = storage.readAll();

        final FieldMask nonEmptyMask = FieldMask.newBuilder()
                                                .addPaths("invalid_path")
                                                .build();
        final Map<I, EntityStorageRecord> masked = storage.readAll(nonEmptyMask);

        assertEmpty(noMask);
        assertEmpty(masked);

        // Same type
        assertEquals(noMask, masked);
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void perform_read_bulk_operations() {
        final int count = 10;
        final List<I> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final I id = newId();
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(id.toString())
                                                 .build();
            final Project state = Project.newBuilder()
                                         .setId(projectId)
                                         .build();

            final Any packedState = AnyPacker.pack(state);

            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(packedState)
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .setVersion(1)
                                                                  .build();
            storage.write(id, record);

            // Request a subset of records
            if (i % 2 == 0) {
                ids.add(id);
            }
        }

        final Iterable<EntityStorageRecord> read = storage.readBulk(ids);
        assertSize(ids.size(), read);

        // Check data consistency
        for (EntityStorageRecord record : read) {
            final Any packedState = record.getState();
            final Project state = AnyPacker.unpack(packedState);
            final ProjectId id = state.getId();
            final String stringIdRepr = id.getId();

            boolean isIdPresent = false;
            for (I genericId : ids) {
                isIdPresent = genericId.toString()
                                       .equals(stringIdRepr);
                if (isIdPresent) {
                    break;
                }
            }
            assertTrue(isIdPresent);
        }
    }

    @Test
    public void perform_bulk_read_with_field_mask_operation() {
        final int count = 10;
        final List<I> ids = new LinkedList<>();

        final String projectDescriptor = Project.getDescriptor()
                                                .getFullName();
        final FieldMask fieldMask = FieldMask.newBuilder()
                                             .addPaths(projectDescriptor + ".id")
                                             .addPaths(projectDescriptor + ".status")
                                             .build();
        for (int i = 0; i < count; i++) {
            final I id = newId();
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(id.toString())
                                                 .build();
            final Project state = Project.newBuilder()
                                         .setId(projectId)
                                         .setName(String.format("project-number-%s", i))
                                         .setStatus(Project.Status.CREATED)
                                         .addTask(Task.getDefaultInstance())
                                         .build();

            final Any packedState = AnyPacker.pack(state);

            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(packedState)
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .setVersion(1)
                                                                  .build();
            storage.write(id, record);

            // Request a subset of records
            if (i % 2 == 0) {
                ids.add(id);
            }
        }

        final Iterable<EntityStorageRecord> read = storage.readBulk(ids, fieldMask);
        assertSize(ids.size(), read);

        // Check data consistency
        for (EntityStorageRecord record : read) {
            final Any packedState = record.getState();
            final Project state = AnyPacker.unpack(packedState);
            final ProjectId id = state.getId();
            final String stringIdRepr = id.getId();

            boolean isIdPresent = false;
            for (I genericId : ids) {
                isIdPresent = genericId.toString()
                                       .equals(stringIdRepr);
                if (isIdPresent) {
                    break;
                }
            }
            assertTrue(isIdPresent);

            assertMatchesMask(state, fieldMask);
        }
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_event_time() {
        storage.writeLastHandledEventTime(Tests.<Timestamp>nullRef());
    }

    @Test
    public void write_and_read_last_event_time() {
        writeAndReadLastEventTimeTest(getCurrentTime());
    }

    @Test
    public void write_and_read_last_event_time_several_times() {
        final Timestamp time1 = getCurrentTime();
        final Timestamp time2 = add(time1, Durations.ofSeconds(10));
        writeAndReadLastEventTimeTest(time1);
        writeAndReadLastEventTimeTest(time2);
    }

    private void writeAndReadLastEventTimeTest(Timestamp expected) {
        storage.writeLastHandledEventTime(expected);

        final Timestamp actual = storage.readLastHandledEventTime();

        assertEquals(expected, actual);
    }
}
