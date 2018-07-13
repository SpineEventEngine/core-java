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

package io.spine.server.storage.memory;

import com.google.protobuf.Message;
import io.spine.server.entity.Entity;
import io.spine.server.storage.RecordStorageTest;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectId;
import io.spine.test.storage.Task;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.server.BoundedContext.newName;
import static java.lang.String.format;
import static java.lang.System.nanoTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("InMemoryRecordStorage should")
public class InMemoryRecordStorageTest
        extends RecordStorageTest<ProjectId, InMemoryRecordStorage<ProjectId>> {

    @Override
    protected InMemoryRecordStorage<ProjectId> newStorage(Class<? extends Entity> cls) {
        StorageSpec<ProjectId> spec = StorageSpec.of(newName(getClass().getSimpleName()),
                                                           TypeUrl.of(Project.class),
                                                           ProjectId.class);
        return InMemoryRecordStorage.newInstance(spec, false, cls);
    }

    @Override
    protected ProjectId newId() {
        ProjectId id = ProjectId.newBuilder()
                                      .setId(newUuid())
                                      .build();
        return id;
    }

    @Override
    protected Message newState(ProjectId id) {
        String uniqueName = format("record-storage-test-%s-%s", id.getId(), nanoTime());
        Project project = Project.newBuilder()
                                       .setId(id)
                                       .setStatus(Project.Status.CREATED)
                                       .setName(uniqueName)
                                       .addTask(Task.getDefaultInstance())
                                       .build();
        return project;
    }

    @Test
    @DisplayName("return storage spec")
    void returnStorageSpec() {
        StorageSpec spec = getStorage().getSpec();
        assertEquals(ProjectId.class, spec.getIdClass());
    }
}
