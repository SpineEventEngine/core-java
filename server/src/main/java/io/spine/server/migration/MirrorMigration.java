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

package io.spine.server.migration;

import io.spine.base.EntityState;
import io.spine.server.BoundedContext;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.storage.Storage;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

/**
 * ...
 */
public final class MirrorMigration {

    public EntityRecord convert(Mirror mirror) {
        return EntityRecord.newBuilder()
                .setEntityId(mirror.getId().getValue())
                .setState(mirror.getState())
                .setVersion(mirror.getVersion())
                .setLifecycleFlags(mirror.getLifecycle())
                .vBuild();
    }

    /**
     * Migrates the {@code Aggregate} states of the specified type from
     * the {@code Mirror}-based storage to the {@code AggregateStorage}.
     *
     * <p> ...
     *
     * @param mirrors
     *         source of {@code Mirror} records
     * @param entityRecords
     *         destination for the migrated records
     * @param observer
     *         observes the migration progress
     * @param <I>
     *         the type of IDs of aggregates which are to be migrated.
     * @param <S>
     *         the type of states of aggregates which are to be migrated
     */
    public <I, S extends EntityState<I>> void run(Storage<MirrorId, Mirror> mirrors,
                                                  EntityRecordStorage<I, S> entityRecords,
                                                  MigrationObserver observer) {
        // To implement.
    }

    /**
     * Migrates {@code Aggregate}s' states from the {@code Mirror}-based storage
     * to the {@code Aggregate}s within the passed {@code BoundedContext}.
     *
     * <p> ...
     *
     * @param mirrors
     *         source of {@code Mirror} records
     * @param context
     *         destination for {@code Mirror} records
     * @param observer
     *         observes the migration progress
     */
    public void run(Storage<MirrorId, Mirror> mirrors,
                    BoundedContext context,
                    MigrationObserver observer) {
        // To implement.
    }
}
