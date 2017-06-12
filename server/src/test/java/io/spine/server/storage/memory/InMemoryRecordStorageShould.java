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

package io.spine.server.storage.memory;

import com.google.protobuf.Message;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.RecordStorageShould;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
import io.spine.type.TypeUrl;

import static io.spine.base.Identifier.newUuid;
import static java.lang.String.format;

/**
 * @author Dmytro Dashenkov
 */
public class InMemoryRecordStorageShould
        extends RecordStorageShould<ProjectId, RecordStorage<ProjectId>> {

    @Override
    protected RecordStorage<ProjectId> getStorage() {
        final StorageSpec<ProjectId> spec = StorageSpec.of(getClass().getSimpleName(),
                                                           TypeUrl.of(Project.class),
                                                           ProjectId.class);
        return InMemoryRecordStorage.newInstance(spec, false);
    }

    @Override
    protected ProjectId newId() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build();
        return id;
    }

    @Override
    protected Message newState(ProjectId id) {
        final Project project = Project.newBuilder()
                                       .setId(id)
                                       .setStatus(Project.Status.CREATED)
                                       .setName(format("record-storage-test-%s", id.getId()))
                                       .addTask(Task.getDefaultInstance())
                                       .build();
        return project;
    }
}
