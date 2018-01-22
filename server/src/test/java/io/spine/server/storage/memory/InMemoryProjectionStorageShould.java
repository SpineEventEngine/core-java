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

package io.spine.server.storage.memory;

import io.spine.server.entity.Entity;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.projection.ProjectionStorageShould;
import io.spine.test.storage.ProjectId;
import io.spine.type.TypeUrl;

import static io.spine.server.BoundedContext.newName;

/**
 * @author Alexander Litus
 */
public class InMemoryProjectionStorageShould extends ProjectionStorageShould {

    @Override
    protected ProjectionStorage<ProjectId> newStorage(Class<? extends Entity> cls) {
        final StorageSpec<ProjectId> spec =
                StorageSpec.of(newName(getClass().getSimpleName()),
                               TypeUrl.of(io.spine.test.projection.Project.class),
                               ProjectId.class);
        final InMemoryProjectionStorage<ProjectId> storage =
                InMemoryProjectionStorage.newInstance(
                        InMemoryRecordStorage.newInstance(spec, false));
        return storage;
    }
}
