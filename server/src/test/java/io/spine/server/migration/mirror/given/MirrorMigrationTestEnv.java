/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.migration.mirror.given;

import io.spine.base.EntityState;
import io.spine.server.ContextSpec;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.migration.mirror.MirrorStorage;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.Storage;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static io.spine.server.migration.mirror.given.MirrorMappingTestEnv.mirror;

public class MirrorMigrationTestEnv {

    private static final String tenantId = MirrorMappingTestEnv.class.getSimpleName();
    private static final ContextSpec contextSpec = ContextSpec.singleTenant(tenantId);

    private MirrorMigrationTestEnv() {
    }

    public static <I, S extends EntityState<I>> EntityRecordStorage<I, S>
    createEntityRecordStorage(Class<? extends Entity<I, S>> entityClass) {
        var storage = InMemoryStorageFactory.newInstance()
                .createEntityRecordStorage(contextSpec, entityClass);
        return storage;
    }

    /**
     * Creates a storage for {@linkplain Mirror} projections.
     *
     * <p>The returned storage is pre-filled with three types of entities'. For each type, number
     * of records to generate is specified.
     */
    @SuppressWarnings({
            "ConstantConditions", /* `Mirror::getId` is a required field, will not return `NULL`. */
            "MethodWithTooManyParameters" /* Convenient for tests. */
    })
    public static MirrorStorage createMirrorStorage(
            Supplier<EntityState<?>> entity1,
            int numberOfEntity1,
            Supplier<EntityState<?>> entity2,
            int numberOfEntity2,
            Supplier<EntityState<?>> entity3,
            int numberOfEntity3
    ) {

        var recordColumn = Mirror.Column.aggregateType();
        var recordSpec = new MessageRecordSpec<>(
                MirrorId.class,
                Mirror.class,
                Mirror::getId,
                List.of(recordColumn)
        );

        MirrorStorage storage = new MirrorStorage.InMemory(contextSpec, recordSpec);
        IntStream.rangeClosed(1, numberOfEntity1)
                .forEach(i -> write(entity1.get(), storage));
        IntStream.rangeClosed(1, numberOfEntity2)
                 .forEach(i -> write(entity2.get(), storage));
        IntStream.rangeClosed(1, numberOfEntity3)
                 .forEach(i -> write(entity3.get(), storage));

        return storage;
    }

    private static <I> void write(EntityState<I> state, Storage<MirrorId, Mirror> storage) {
        var mirror = mirror(state);
        storage.write(mirror.getId(), mirror);
    }
}
