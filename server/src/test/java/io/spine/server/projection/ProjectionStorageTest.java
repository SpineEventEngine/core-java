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

package io.spine.server.projection;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.given.GivenVersion;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.RecordStorageShould;
import io.spine.test.Tests;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.protobuf.util.Durations.fromSeconds;
import static com.google.protobuf.util.Timestamps.add;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.test.Tests.assertMatchesMask;
import static io.spine.test.Verify.assertContains;
import static io.spine.test.Verify.assertSize;
import static io.spine.test.Verify.assertThrows;
import static io.spine.testdata.TestEntityStorageRecordFactory.newEntityStorageRecord;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Projection storage tests.
 *
 * @author Alexander Litus
 */
public abstract class ProjectionStorageTest
        extends RecordStorageShould<ProjectId, ProjectionStorage<ProjectId>> {

    private ProjectionStorage<ProjectId> storage;

    @SuppressWarnings("BreakStatement")
    @CanIgnoreReturnValue
    private static Project checkProjectIdIsInList(EntityRecord project, List<ProjectId> ids) {
        Any packedState = project.getState();
        Project state = AnyPacker.unpack(packedState);
        ProjectId id = state.getId();

        boolean isIdPresent = false;
        for (ProjectId genericId : ids) {
            isIdPresent = genericId.equals(id);
            if (isIdPresent) {
                break;
            }
        }
        assertTrue(isIdPresent);

        return state;
    }

    private static FieldMask maskForPaths(String... paths) {
        FieldMask mask = FieldMask
                .newBuilder()
                .addAllPaths(Arrays.asList(paths))
                .build();
        return mask;
    }

    @Override
    protected Message newState(ProjectId id) {
        String uniqueName = format("Projection_name-%s-%s", id.getId(), System.nanoTime());
        Project state = Given.project(id, uniqueName);
        return state;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    protected EntityRecord newStorageRecord() {
        return newEntityStorageRecord();
    }

    @Override
    protected ProjectId newId() {
        return ProjectId.newBuilder()
                        .setId(newUuid())
                        .build();
    }

    @BeforeEach
    public void setUpProjectionStorageTest() {
        storage = getStorage();
    }

    @AfterEach
    public void tearDownProjectionStorageTest() {
        close(storage);
    }

    @Test
    @DisplayName("return null if no event time in storage")
    void returnNullIfNoEventTimeInStorage() {
        Timestamp time = storage.readLastHandledEventTime();

        assertNull(time);
    }

    @Test
    @DisplayName("read all messages")
    void readAllMessages() {
        List<ProjectId> ids = fillStorage(5);

        Iterator<EntityRecord> read = storage.readAll();
        Collection<EntityRecord> readRecords = newArrayList(read);
        assertSize(ids.size(), readRecords);
        for (EntityRecord record : readRecords) {
            Project state = AnyPacker.unpack(record.getState());
            ProjectId id = state.getId();
            assertContains(id, ids);
        }
    }

    @Test
    @DisplayName("read all messages with field mask")
    void readAllMessagesWithFieldMask() {
        List<ProjectId> ids = fillStorage(5);

        String projectDescriptor = Project.getDescriptor()
                                          .getFullName();
        @SuppressWarnings("DuplicateStringLiteralInspection")
        // clashes with non-related tests.
                FieldMask fieldMask = maskForPaths(projectDescriptor + ".id",
                                                   projectDescriptor + ".name");

        Iterator<EntityRecord> read = storage.readAll(fieldMask);
        Collection<EntityRecord> readRecords = newArrayList(read);
        assertSize(ids.size(), readRecords);
        for (EntityRecord record : readRecords) {
            Any packedState = record.getState();
            Project state = AnyPacker.unpack(packedState);
            assertMatchesMask(state, fieldMask);
        }
    }

    @Test
    @DisplayName("perform read bulk operations")
    void performReadBulkOperations() {
        // Get a subset of IDs
        List<ProjectId> ids = fillStorage(10).subList(0, 5);

        Iterator<EntityRecord> read = storage.readMultiple(ids);
        Collection<EntityRecord> readRecords = newArrayList(read);
        assertSize(ids.size(), readRecords);

        // Check data consistency
        for (EntityRecord record : readRecords) {
            checkProjectIdIsInList(record, ids);
        }
    }

    @Test
    @DisplayName("perform bulk read with field mask operation")
    void performBulkReadWithFieldMaskOperation() {
        // Get a subset of IDs
        List<ProjectId> ids = fillStorage(10).subList(0, 5);

        String projectDescriptor = Project.getDescriptor()
                                          .getFullName();
        FieldMask fieldMask = maskForPaths(projectDescriptor + ".id",
                                           projectDescriptor + ".status");

        Iterator<EntityRecord> read = storage.readMultiple(ids, fieldMask);
        Collection<EntityRecord> readRecords = newArrayList(read);
        assertSize(ids.size(), readRecords);

        // Check data consistency
        for (EntityRecord record : readRecords) {
            Project state = checkProjectIdIsInList(record, ids);
            assertMatchesMask(state, fieldMask);
        }
    }

    @Test
    @DisplayName("throw exception if write null event time")
    void throwExceptionIfWriteNullEventTime() {
        assertThrows(NullPointerException.class,
                     () -> storage.writeLastHandledEventTime(Tests.nullRef()));
    }

    @Test
    @DisplayName("write and read last event time")
    void writeAndReadLastEventTime() {
        writeAndReadLastEventTimeTest(getCurrentTime());
    }

    @Test
    @DisplayName("write and read last event time several times")
    void writeAndReadLastEventTimeSeveralTimes() {
        Timestamp time1 = getCurrentTime();
        Timestamp time2 = add(time1, fromSeconds(10L));
        writeAndReadLastEventTimeTest(time1);
        writeAndReadLastEventTimeTest(time2);
    }

    @SuppressWarnings("ConstantConditions") // Converter nullability issues
    private List<ProjectId> fillStorage(int count) {
        List<ProjectId> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            ProjectId id = newId();
            Project state = Given.project(id, format("project-%d", i));
            Any packedState = AnyPacker.pack(state);

            EntityRecord rawRecord = EntityRecord
                    .newBuilder()
                    .setState(packedState)
                    .setVersion(GivenVersion.withNumber(1))
                    .build();
            EntityRecordWithColumns record = withLifecycleColumns(rawRecord);
            storage.write(id, record);
            ids.add(id);
        }

        return ids;
    }

    private void writeAndReadLastEventTimeTest(Timestamp expected) {
        storage.writeLastHandledEventTime(expected);

        Timestamp actual = storage.readLastHandledEventTime();

        assertEquals(expected, actual);
    }

    private static class Given {

        private static Project project(ProjectId id, String name) {
            Project project = Project
                    .newBuilder()
                    .setId(id)
                    .setName(name)
                    .setStatus(Project.Status.CREATED)
                    .addTask(Task.getDefaultInstance())
                    .build();
            return project;
        }
    }
}
