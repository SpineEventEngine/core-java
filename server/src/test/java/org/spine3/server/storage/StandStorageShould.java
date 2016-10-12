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

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.protobuf.Any;
import org.junit.Test;
import org.spine3.protobuf.AnyPacker;
import org.spine3.protobuf.Timestamps;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.stand.AggregateStateId;
import org.spine3.test.projection.Project;
import org.spine3.test.projection.ProjectId;
import org.spine3.test.projection.Task;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.spine3.test.Verify.assertContains;
import static org.spine3.test.Verify.assertSize;

/**
 * @author Dmytro Dashenkov
 */
public abstract class StandStorageShould {

    protected abstract StandStorage createStorage();

    @Test
    public void retrieve_all_records() {
        final StandStorage storage = createStorage();
        final List<AggregateStateId<ProjectId>> ids = fill(storage, 10, new Supplier<AggregateStateId<ProjectId>>() {
            @SuppressWarnings("unchecked")
            @Override
            public AggregateStateId<ProjectId> get() {
                final ProjectId projectId = ProjectId.newBuilder()
                                                     .setId(UUID.randomUUID()
                                                                .toString())
                                                     .build();
                return AggregateStateId.of(projectId, TypeUrl.of(Project.class));
            }
        });

        final Map<AggregateStateId, EntityStorageRecord> allRecords = storage.readAll();

        checkIds(ids, allRecords.values());
    }

    protected static List<AggregateStateId<ProjectId>> fill(StandStorage storage,
                                                            int count,
                                                            Supplier<AggregateStateId<ProjectId>> idSupplier) {
        final List<AggregateStateId<ProjectId>> ids = new LinkedList<>();

        for (int i = 0; i < count; i++) {
            final AggregateStateId<ProjectId> genericId = idSupplier.get();
            final ProjectId id = genericId.getAggregateId();
            final Project project = Project.newBuilder()
                                           .setId(id)
                                           .setStatus(Project.Status.CREATED)
                                           .setName(String.format("test-project-%s", i))
                                           .addTask(Task.getDefaultInstance())
                                           .build();
            final EntityStorageRecord record = EntityStorageRecord.newBuilder()
                                                                  .setState(AnyPacker.pack(project))
                                                                  .setWhenModified(Timestamps.getCurrentTime())
                                                                  .setVersion(1)
                                                                  .build();
            storage.write(genericId, record);
            ids.add(genericId);
        }

        return ids;
    }

    protected void checkIds(List<AggregateStateId<ProjectId>> ids, Collection<EntityStorageRecord> records) {
        assertSize(ids.size(), records);

        final Collection<ProjectId> projectIds = Collections2.transform(ids, new Function<AggregateStateId<ProjectId>, ProjectId>() {
            @Nullable
            @Override
            public ProjectId apply(@Nullable AggregateStateId<ProjectId> input) {
                if (input == null) {
                    return null;
                }
                return input.getAggregateId();
            }
        });

        for (EntityStorageRecord record : records) {
            final Any packedState = record.getState();
            final Project state = AnyPacker.unpack(packedState);
            final ProjectId id = state.getId();

            assertContains(id, projectIds);
        }
    }

}
