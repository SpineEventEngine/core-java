/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionStorage;
import io.spine.server.projection.ProjectionStorageTest;
import io.spine.test.storage.ProjectId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;

import static io.spine.core.BoundedContextNames.newName;

@DisplayName("InMemoryProjectionStorage should")
class InMemoryProjectionStorageTest extends ProjectionStorageTest {

    @SuppressWarnings("unchecked") // Logically correct.
    @Override
    protected ProjectionStorage<ProjectId> newStorage(Class<? extends Entity<?, ?>> cls) {
        StorageSpec<ProjectId> spec =
                StorageSpec.of(newName(getClass().getSimpleName()),
                               TypeUrl.of(io.spine.test.projection.Project.class),
                               ProjectId.class);
        InMemoryProjectionStorage<ProjectId> storage =
                new InMemoryProjectionStorage<>(
                        (Class<? extends Projection<?,?,?>>) cls,
                        new InMemoryRecordStorage<>(spec, cls, false)
                );
        return storage;
    }
}
