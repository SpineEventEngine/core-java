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

package io.spine.server.migration.mirror;

import com.google.common.collect.Lists;
import io.spine.base.EntityState;
import io.spine.query.RecordQuery;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

import java.util.stream.Collectors;

/**
 * Migrates {@linkplain Mirror} projections into
 * {@linkplain io.spine.server.entity.storage.EntityRecordWithColumns EntityRecordWithColumns}.
 *
 * <p>{@code Mirror} was deprecated in Spine 2.x. Now, {@code EntityRecordWithColumns} is used
 * to store the aggregate's state for further querying.
 *
 * @param <I>
 *         the aggregate's identifier
 * @param <S>
 *         the aggregate's state
 * @param <A>
 *         the aggregate's class
 */
public final class MirrorMigration<I, S extends EntityState<I>, A extends Aggregate<I, S, ?>> {

    private final EntityRecordStorage<I, S> entityRecordStorage;
    private final MirrorStorage mirrorStorage;
    private final MirrorMapping<I, S, A> mapping;
    private final RecordQuery<MirrorId, Mirror> nextBatch;
    private final MigrationSupervisor supervisor;

    public MirrorMigration(ContextSpec contextSpec,
                           StorageFactory storageFactory,
                           Class<A> aggregateClass,
                           MigrationSupervisor supervisor) {

        this.entityRecordStorage = storageFactory
                .createEntityRecordStorage(contextSpec, aggregateClass);
        this.mirrorStorage = new MirrorStorage(contextSpec, storageFactory);
        this.mapping = new MirrorMapping<>(aggregateClass);
        this.nextBatch = composeQuery(mirrorStorage, aggregateClass, supervisor.stepSize());
        this.supervisor = supervisor;

    }

    private static <I, S extends EntityState<I>, A extends Aggregate<I, S, ?>>
    RecordQuery<MirrorId, Mirror> composeQuery(MirrorStorage mirrorStorage,
                                               Class<A> aggregateClass,
                                               int batchSize) {

        var aggregateType = AggregateClass.asAggregateClass(aggregateClass)
                                          .stateTypeUrl()
                                          .value();
        var nextBatch = mirrorStorage.queryBuilder()
                                      .where(Mirror.Column.aggregateType())
                                      .is(aggregateType)
                                      .where(Mirror.Column.wasMigrated())
                                      .is(false)
                                      .limit(batchSize)
                                      .build();
        return nextBatch;
    }

    /**
     * Migrates {@linkplain Mirror} projections which belong to the specified aggregate
     * to the {@linkplain EntityRecordStorage} of that aggregate.
     */
    public void run() {

        supervisor.onMigrationStarted();

        ensureWasMigratedColumn(); // ??

        var completedStep = proceed();
        while (completedStep.getMigrated() == supervisor.stepSize()
                        && supervisor.shouldContinueAfter(completedStep)) {
            completedStep = proceed();
        }

        supervisor.onMigrationCompleted();
    }

    private void ensureWasMigratedColumn() {

        // Goes through all the messages, sets `wasMigrated=false` if the column is not set.

        // Could it be a responsibility of `MirrorStorage` to ensure the column is present, and the
        // default value = `false`?

        // For table-based - as simple as a single query.
        // For record-based - batch updated is required, which is sort of impossible.
    }

    private MigrationStep proceed() {

        supervisor.onStepStarted();

        var mirrorRecords = Lists.newArrayList(mirrorStorage.readAll(nextBatch));
        var entityRecords = mirrorRecords.stream()
                .map(mapping::toEntityRecord)
                .collect(Collectors.toList());
        var migratedMirrorRecords = mirrorRecords.stream()
                .map(mirror -> mirror.toBuilder().setWasMigrated(true).build())
                .collect(Collectors.toList());

        entityRecordStorage.writeAll(entityRecords);
        mirrorStorage.writeBatch(migratedMirrorRecords);

        var completedStep = MigrationStep.newBuilder()
                .setMigrated(migratedMirrorRecords.size())
                .build();

        supervisor.onStepCompleted(completedStep);
        return completedStep;
    }
}
