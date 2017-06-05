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

package io.spine.server.projection;

import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.storage.RecordStorageShould;
import io.spine.test.Tests;
import io.spine.test.projection.Project;
import io.spine.test.projection.ProjectId;
import io.spine.test.projection.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.protobuf.util.Durations.fromSeconds;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.test.Tests.assertMatchesMask;
import static io.spine.test.Verify.assertContains;
import static io.spine.test.Verify.assertEmpty;
import static io.spine.test.Verify.assertSize;
import static io.spine.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;
import static io.spine.time.Time.getCurrentTime;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Projection storage tests.
 *
 * @param <I> the type of IDs of storage records
 * @author Alexander Litus
 */
public abstract class ProjectionStorageShould<I>
        extends RecordStorageShould<I, ProjectionStorage<I>> {

    private ProjectionStorage<I> storage;

    @Override
    protected Message newState(I id) {
        final String projectId = id.getClass()
                                   .getName();
        final Project state = Given.project(projectId,"Projection name " + projectId);
        return state;
    }

    @Before
    public void setUpProjectionStorageTest() {
        storage = getStorage();
    }

    @After
    public void tearDownProjectionStorageTest() {
        close(storage);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected EntityRecord newStorageRecord() {
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
        final List<I> ids = fillStorage(5);

        final Map<I, EntityRecord> read = storage.readAll();
        assertSize(ids.size(), read);
        for (Map.Entry<I, EntityRecord> record : read.entrySet()) {
            assertContains(record.getKey(), ids);
        }
    }

    @SuppressWarnings("MethodWithMultipleLoops")
    @Test
    public void read_all_messages_with_field_mask() {
        final List<I> ids = fillStorage(5);

        final String projectDescriptor = Project.getDescriptor()
                                                .getFullName();
        @SuppressWarnings("DuplicateStringLiteralInspection")       // clashes with non-related tests.
        final FieldMask fieldMask = maskForPaths(projectDescriptor + ".id", projectDescriptor + ".name");

        final Map<I, EntityRecord> read = storage.readAll(fieldMask);
        assertSize(ids.size(), read);
        for (Map.Entry<I, EntityRecord> record : read.entrySet()) {
            assertContains(record.getKey(), ids);

            final Any packedState = record.getValue()
                                          .getState();
            final Project state = AnyPacker.unpack(packedState);
            assertMatchesMask(state, fieldMask);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod") // because the behaviour to test is different
    @Override
    @Test
    public void retrieve_empty_map_if_storage_is_empty() {
        final Map<I, EntityRecord> noMaskEntiries = storage.readAll();

        final FieldMask nonEmptyMask = FieldMask.newBuilder()
                                                .addPaths("invalid_path")
                                                .build();
        final Map<I, EntityRecord> maskedEntries = storage.readAll(nonEmptyMask);

        assertEmpty(noMaskEntiries);
        assertEmpty(maskedEntries);

        // Same type
        assertEquals(noMaskEntiries, maskedEntries);
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "BreakStatement"})
    @Test
    public void perform_read_bulk_operations() {
        // Get a subset of IDs
        final List<I> ids = fillStorage(10).subList(0, 5);

        final Iterable<EntityRecord> read = storage.readMultiple(ids);
        assertSize(ids.size(), read);

        // Check data consistency
        for (EntityRecord record : read) {
            checkProjectIdIsInList(record, ids);
        }
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "BreakStatement"})
    @Test
    public void perform_bulk_read_with_field_mask_operation() {
        // Get a subset of IDs
        final List<I> ids = fillStorage(10).subList(0, 5);

        final String projectDescriptor = Project.getDescriptor()
                                                .getFullName();
        final FieldMask fieldMask = maskForPaths(projectDescriptor + ".id", projectDescriptor + ".status");

        final Iterable<EntityRecord> read = storage.readMultiple(ids, fieldMask);
        assertSize(ids.size(), read);

        // Check data consistency
        for (EntityRecord record : read) {
            final Project state = checkProjectIdIsInList(record, ids);
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
        final Timestamp time2 = add(time1, fromSeconds(10L));
        writeAndReadLastEventTimeTest(time1);
        writeAndReadLastEventTimeTest(time2);
    }

    @SuppressWarnings("ConstantConditions") // Converter nullability issues
    private List<I> fillStorage(int count) {
        final List<I> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final I id = newId();
            final Project state = Given.project(id.toString(), format("project-%d", i));
            final Any packedState = AnyPacker.pack(state);

            final EntityRecord record = EntityRecord.newBuilder()
                                                    .setState(packedState)
                                                    .setVersion(Tests.newVersionWithNumber(1))
                                                    .build();
            storage.write(id, record);
            ids.add(id);
        }

        return ids;
    }

    private void writeAndReadLastEventTimeTest(Timestamp expected) {
        storage.writeLastHandledEventTime(expected);

        final Timestamp actual = storage.readLastHandledEventTime();

        assertEquals(expected, actual);
    }

    @SuppressWarnings("BreakStatement")
    private static <I> Project checkProjectIdIsInList(EntityRecord project, List<I> ids) {
        final Any packedState = project.getState();
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

        return state;
    }

    private static FieldMask maskForPaths(String... paths) {
        final FieldMask mask = FieldMask.newBuilder()
                                        .addAllPaths(Arrays.asList(paths))
                                        .build();
        return mask;
    }

    private static class Given {

        private static Project project(String id, String name) {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(id)
                                                 .build();
            final Project project = Project.newBuilder()
                                           .setId(projectId)
                                           .setName(name)
                                           .setStatus(Project.Status.CREATED)
                                           .addTask(Task.getDefaultInstance())
                                           .build();
            return project;
        }
    }
}
