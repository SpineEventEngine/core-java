/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.FieldMask;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.core.given.GivenVersion;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.FieldMasks;
import io.spine.server.storage.RecordStorageShould;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
import io.spine.test.storage.TaskId;
import io.spine.type.TypeUrl;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static io.spine.test.Tests.assertMatchesMask;
import static io.spine.test.Verify.assertContains;
import static io.spine.test.Verify.assertSize;
import static java.lang.String.format;

/**
 * @author Dmytro Dashenkov
 */
public abstract class StandStorageShould extends RecordStorageShould<AggregateStateId,
                                                                     StandStorage> {

    protected static final Supplier<AggregateStateId<ProjectId>> DEFAULT_ID_SUPPLIER
            = new Supplier<AggregateStateId<ProjectId>>() {
        @SuppressWarnings("unchecked")
        @Override
        public AggregateStateId<ProjectId> get() {
            final ProjectId projectId = ProjectId.newBuilder()
                                                 .setId(Identifier.newUuid())
                                                 .build();
            return AggregateStateId.of(projectId, TypeUrl.of(Project.class));
        }
    };

    @Override
    protected Message newState(AggregateStateId id) {
        final String uniqueName = format("test-project-%s-%s", id.toString(), System.nanoTime());
        final Project project = Project.newBuilder()
                                       .setId((ProjectId) id.getAggregateId())
                                       .setStatus(Project.Status.CREATED)
                                       .setName(uniqueName)
                                       .addTask(Task.getDefaultInstance())
                                       .build();
        return project;
    }

    @Test
    public void retrieve_all_records() {
        final StandStorage storage = getStorage();
        final List<AggregateStateId> ids = fill(storage, 10, DEFAULT_ID_SUPPLIER);

        final Iterator<EntityRecord> allRecords = storage.readAll();
        checkIds(ids, allRecords);
    }

    @Test
    public void retrieve_records_by_ids() {
        final StandStorage storage = getStorage();
        // Use a subset of IDs
        final List<AggregateStateId> ids = fill(storage, 10, DEFAULT_ID_SUPPLIER).subList(0, 5);

        final Iterator<EntityRecord> records = storage.readMultiple(ids);
        checkIds(ids, records);
    }

    @Test
    public void read_all_records_of_given_type() {
        checkByTypeRead(FieldMask.getDefaultInstance());
    }

    @Test
    public void read_all_records_of_given_type_with_field_mask() {
        final FieldMask mask = FieldMasks.maskOf(Project.getDescriptor(), 1, 2);
        checkByTypeRead(mask);
    }

    @SuppressWarnings({"MethodWithMultipleLoops", "ConstantConditions"}) // OK for this test.
    private void checkByTypeRead(FieldMask fieldMask) {
        final boolean withFieldMask = !fieldMask.equals(FieldMask.getDefaultInstance());
        final StandStorage storage = getStorage();
        final TypeUrl type = TypeUrl.from(Project.getDescriptor());

        final int projectsCount = 4;
        final List<AggregateStateId> projectIds = fill(storage, projectsCount, DEFAULT_ID_SUPPLIER);

        final int tasksCount = 5;
        for (int i = 0; i < tasksCount; i++) {
            final TaskId genericId = TaskId.newBuilder()
                                           .setId(i)
                                           .build();
            final AggregateStateId id = AggregateStateId.of(genericId, TypeUrl.from(Task.getDescriptor()));
            final Task task = Task.newBuilder()
                                  .setTaskId(genericId)
                                  .setTitle("Test task")
                                  .setDescription("With description")
                                  .build();
            final EntityRecord record = newRecord(task);
            storage.write(id, record);
        }

        final Iterator<EntityRecord> readRecords
                = withFieldMask
                  ? storage.readAllByType(TypeUrl.from(Project.getDescriptor()), fieldMask)
                  : storage.readAllByType(TypeUrl.from(Project.getDescriptor()));
        final Set<EntityRecord> readDistinct = Sets.newHashSet(readRecords);
        assertSize(projectsCount, readDistinct);

        for (EntityRecord record : readDistinct) {
            final Any state = record.getState();
            final Project project = AnyPacker.unpack(state);
            final AggregateStateId restored = AggregateStateId.of(project.getId(), type);
            assertContains(restored, projectIds);

            if (withFieldMask) {
                assertMatchesMask(project, fieldMask);
            }
        }
    }

    /*
     * Disabled test cases from `RecordStorageShould`.
     *
     * These tests check the entity column reads and writes, which are not applicable to
     * `StandStorage`.
     *
     * These checks include the `LifecycleFlags`-related checks (`archived` and `deleted`).
     */

    @SuppressWarnings("NoopMethodInAbstractClass") // Overrides the behavior for all the inheritors.
    @Override
    public void filter_records_by_columns() {
        // Stand storage does not support entity columns.
    }

    @SuppressWarnings("NoopMethodInAbstractClass") // Overrides the behavior for all the inheritors.
    @Override
    public void allow_by_single_id_queries_with_no_columns() {
        // Stand storage does not support entity columns.
    }

    @SuppressWarnings("NoopMethodInAbstractClass") // Overrides the behavior for all the inheritors.
    @Override
    public void read_archived_records_if_specified() {
        // Stand storage does not support entity columns.
    }

    @SuppressWarnings("NoopMethodInAbstractClass") // Overrides the behavior for all the inheritors.
    @Override
    public void filter_archived_or_deleted_records_on_by_ID_bulk_read() {
        // Stand storage does not support entity columns.
    }

    @SuppressWarnings("NoopMethodInAbstractClass") // Overrides the behavior for all the inheritors.
    @Override
    public void update_entity_column_values() {
        // Stand storage does not support entity columns.
    }

    @SuppressWarnings("NoopMethodInAbstractClass") // Overrides the behavior for all the inheritors.
    @Override
    public void read_both_by_columns_and_IDs() {
        // Stand storage does not support entity columns.
    }

    @Override
    protected AggregateStateId newId() {
        return DEFAULT_ID_SUPPLIER.get();
    }

    @SuppressWarnings("ConstantConditions") // Converter nullability issues
    protected List<AggregateStateId> fill(StandStorage storage,
                                          int count,
                                          Supplier<AggregateStateId<ProjectId>> idSupplier) {
        final List<AggregateStateId> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final AggregateStateId genericId = idSupplier.get();
            final Message state = newState(genericId);
            final EntityRecord record = newRecord(state);
            storage.write(genericId, record);
            ids.add(genericId);
        }

        return ids;
    }

    private static EntityRecord newRecord(Message state) {
        final EntityRecord record = EntityRecord.newBuilder()
                                                .setState(AnyPacker.pack(state))
                                                .setVersion(GivenVersion.withNumber(1))
                                                .build();
        return record;
    }

    protected void checkIds(List<AggregateStateId> ids, Iterator<EntityRecord> records) {
        final Collection<EntityRecord> recordsToCheck = newArrayList(records);
        assertSize(ids.size(), recordsToCheck);

        final Collection<ProjectId> projectIds = Collections2.transform(ids, new Function<AggregateStateId, ProjectId>() {
            @Nullable
            @Override
            public ProjectId apply(@Nullable AggregateStateId input) {
                if (input == null) {
                    return null;
                }
                return (ProjectId) input.getAggregateId();
            }
        });

        for (EntityRecord record : recordsToCheck) {
            final Any packedState = record.getState();
            final Project state = AnyPacker.unpack(packedState);
            final ProjectId id = state.getId();

            assertContains(id, projectIds);
        }
    }
}
