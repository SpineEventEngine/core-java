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

package org.spine3.server.storage.memory;

import com.google.protobuf.Message;
import org.spine3.base.Identifiers;
import org.spine3.server.storage.AbstractStorage;
import org.spine3.server.storage.EntityStorageRecord;
import org.spine3.server.storage.RecordStorageShould;
import org.spine3.test.storage.Project;
import org.spine3.test.storage.ProjectId;
import org.spine3.test.storage.Task;

/**
 * @author Dmytro Dashenkov
 */
public class InMemoryRecordStorageShould extends RecordStorageShould<ProjectId> {

    @SuppressWarnings("unchecked")
    @Override
    protected AbstractStorage<ProjectId, EntityStorageRecord> getStorage() {
        return InMemoryRecordStorage.newInstance(false);
    }

    @Override
    protected ProjectId newId() {
        final ProjectId id = ProjectId.newBuilder()
                                      .setId(Identifiers.newUuid())
                                      .build();
        return id;
    }

    @Override
    protected Message newState(ProjectId id) {
        final Project project = Project.newBuilder()
                                       .setId(id)
                                       .setStatus(Project.Status.CREATED)
                                       .setName(String.format("record-storage-test-%s", id.getId()))
                                       .addTask(Task.getDefaultInstance())
                                       .build();
        return project;
    }
}
