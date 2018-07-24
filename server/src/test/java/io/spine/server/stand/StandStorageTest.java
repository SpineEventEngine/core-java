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

package io.spine.server.stand;

import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.Entity;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.FieldMasks;
import io.spine.server.storage.AbstractRecordStorateTest;
import io.spine.server.storage.given.StandStorageTestEnv;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
import io.spine.test.storage.TaskId;
import io.spine.testing.core.given.GivenVersion;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.testing.Tests.assertMatchesMask;
import static io.spine.testing.Verify.assertContains;
import static io.spine.testing.Verify.assertSize;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * @author Dmytro Dashenkov
 */
@SuppressWarnings("unused") // JUnit nested classes considered unused in abstract class.
public abstract class StandStorageTest
        extends AbstractRecordStorateTest<AggregateStateId, StandStorage> {

    @Override
    protected Class<? extends Entity> getTestEntityClass() {
        return StandStorageTestEnv.StandingEntity.class;
    }

    @Override
    protected AggregateStateId newId() {
        ProjectId projectId = ProjectId
                .newBuilder()
                .setId(Identifier.newUuid())
                .build();
        return AggregateStateId.of(projectId, TypeUrl.of(Project.class));
    }

    @Override
    protected Message newState(AggregateStateId id) {
        String uniqueName = format("test-project-%s-%s", id.toString(), System.nanoTime());
        Project project = Project
                .newBuilder()
                .setId((ProjectId) id.getAggregateId())
                .setStatus(Project.Status.CREATED)
                .setName(uniqueName)
                .addTask(Task.getDefaultInstance())
                .build();
        return project;
    }

    @Nested
    @DisplayName("Read records")
    class ReadRecords {

        private EntityRecord newRecord(Message state) {
            EntityRecord record = EntityRecord
                    .newBuilder()
                    .setState(AnyPacker.pack(state))
                    .setVersion(GivenVersion.withNumber(1))
                    .build();
            return record;
        }

        @Test
        @DisplayName("all")
        void all() {
            StandStorage storage = getStorage();
            List<AggregateStateId> ids = fill(storage, 10, StandStorageTest.this::newId);

            Iterator<EntityRecord> allRecords = storage.readAll();
            checkIds(ids, allRecords);
        }

        @Test
        @DisplayName("by IDs")
        void byIds() {
            StandStorage storage = getStorage();
            // Use a subset of IDs
            List<AggregateStateId> ids =
                    fill(storage, 10, StandStorageTest.this::newId)
                    .subList(0, 5);

            Iterator<EntityRecord> records = storage.readMultiple(ids);
            checkIds(ids, records);
        }

        @Test
        @DisplayName("by type")
        void byType() {
            checkByTypeRead(FieldMask.getDefaultInstance());
        }

        @Test
        @DisplayName("by type with field mask")
        void byTypeAndFieldMask() {
            FieldMask mask = FieldMasks.maskOf(Project.getDescriptor(), 1, 2);
            checkByTypeRead(mask);
        }

        private void checkByTypeRead(FieldMask fieldMask) {
            boolean withFieldMask = !fieldMask.equals(FieldMask.getDefaultInstance());
            StandStorage storage = getStorage();
            TypeUrl type = TypeUrl.from(Project.getDescriptor());

            int projectsCount = 4;
            List<AggregateStateId> projectIds =
                    fill(storage, projectsCount, StandStorageTest.this::newId);

            int tasksCount = 5;
            createAndWriteTasks(tasksCount);

            Iterator<EntityRecord> readRecords
                    = withFieldMask
                      ? storage.readAllByType(TypeUrl.from(Project.getDescriptor()), fieldMask)
                      : storage.readAllByType(TypeUrl.from(Project.getDescriptor()));
            Set<EntityRecord> readDistinct = Sets.newHashSet(readRecords);
            assertSize(projectsCount, readDistinct);

            for (EntityRecord record : readDistinct) {
                Any state = record.getState();
                Project project = AnyPacker.unpack(state);
                AggregateStateId restored = AggregateStateId.of(project.getId(), type);
                assertContains(restored, projectIds);

                if (withFieldMask) {
                    assertMatchesMask(project, fieldMask);
                }
            }
        }

        private void createAndWriteTasks(int tasksCount) {
            for (int i = 0; i < tasksCount; i++) {
                createAndWriteTask(i);
            }
        }

        private void createAndWriteTask(int i) {
            StandStorage storage = getStorage();
            TaskId genericId = TaskId
                    .newBuilder()
                    .setId(i)
                    .build();
            AggregateStateId id =
                    AggregateStateId.of(genericId, TypeUrl.from(Task.getDescriptor()));
            Task task = Task
                    .newBuilder()
                    .setTaskId(genericId)
                    .setTitle("Test task")
                    .setDescription("With description")
                    .build();
            EntityRecord record = newRecord(task);
            storage.write(id, record);
        }

        private List<AggregateStateId> fill(StandStorage storage,
                                            int count,
                                            Supplier<AggregateStateId<ProjectId>> idSupplier) {
            List<AggregateStateId> ids = new LinkedList<>();

            for (int i = 0; i < count; i++) {
                AggregateStateId genericId = idSupplier.get();
                Message state = newState(genericId);
                EntityRecord record = newRecord(state);
                storage.write(genericId, record);
                ids.add(genericId);
            }

            return ids;
        }

        private void checkIds(List<AggregateStateId> ids, Iterator<EntityRecord> records) {
            Collection<EntityRecord> recordsToCheck = newArrayList(records);
            assertSize(ids.size(), recordsToCheck);

            Collection<ProjectId> projectIds =
                    ids.stream()
                       .map((i) -> (ProjectId) i.getAggregateId())
                       .collect(toList());

            for (EntityRecord record : recordsToCheck) {
                Any packedState = record.getState();
                Project state = AnyPacker.unpack(packedState);
                ProjectId id = state.getId();

                assertContains(id, projectIds);
            }
        }
    }
}
