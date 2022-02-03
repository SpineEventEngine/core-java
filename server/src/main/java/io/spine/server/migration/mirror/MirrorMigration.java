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

import com.google.common.annotations.VisibleForTesting;
import io.spine.base.EntityState;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.entity.storage.EntityRecordWithColumns;
import io.spine.server.storage.StorageFactory;
import io.spine.system.server.Mirror;

import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
@Immutable
public final class MirrorMigration<I, S extends EntityState<I>, A extends Aggregate<I, S, ?>> {

    private final EntityRecordStorage<I, S> entityRecordStorage;
    private final MirrorStorage mirrorStorage;
    private final MirrorMapping<I, S, A> mapping;
    private final String aggregateType;

    public MirrorMigration(ContextSpec contextSpec,
                           StorageFactory storageFactory,
                           Class<A> aggClass) {

        this.entityRecordStorage = storageFactory.createEntityRecordStorage(contextSpec, aggClass);
        this.mirrorStorage = new MirrorStorage(contextSpec, storageFactory);
        this.mapping = new MirrorMapping<>(aggClass);
        this.aggregateType = AggregateClass.asAggregateClass(aggClass)
                                          .stateTypeUrl()
                                          .value();
    }

    /**
     * Migrates {@linkplain Mirror} projections which belong to the specified aggregate
     * to the {@linkplain EntityRecordStorage} of that aggregate.
     */
    public void run(MigrationSupervisor supervisor) {

        supervisor.onMigrationStarted();

        ensureWasMigratedColumn(); // ??

        var completedStep = proceed(supervisor);
        while (completedStep.getMigrated() == supervisor.batchSize()
                        && supervisor.shouldContinueAfter(completedStep)) {
            completedStep = proceed(supervisor);
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

    private MigrationStep proceed(MigrationSupervisor supervisor) {

        supervisor.onStepStarted();

        var batchSize = supervisor.batchSize();
        Collection<EntityRecordWithColumns<I>> entityRecords = new ArrayList<>(batchSize);
        Collection<Mirror> migratedMirrors = new ArrayList<>(batchSize);

        fetchNextBatch(batchSize)
                .forEachRemaining(mirror -> {

                    var entityRecord = mapping.toEntityRecord(mirror);
                    var migratedMirror = mirror.toBuilder()
                            .setWasMigrated(true)
                            .build();

                    entityRecords.add(entityRecord);
                    migratedMirrors.add(migratedMirror);
                });

        entityRecordStorage.writeAll(entityRecords);
        mirrorStorage.writeBatch(migratedMirrors);

        var completedStep = MigrationStep.newBuilder()
                .setMigrated(migratedMirrors.size())
                .build();

        supervisor.onStepCompleted(completedStep);
        return completedStep;
    }

    private Iterator<Mirror> fetchNextBatch(int batchSize) {
        var query = mirrorStorage.queryBuilder()
                                 .where(Mirror.Column.aggregateType()).is(aggregateType)
                                 .where(Mirror.Column.wasMigrated()).is(false)
                                 .sortAscendingBy(Mirror.Column.wasMigrated())
                                 .limit(batchSize)
                                 .build();
        var iterator = mirrorStorage.readAll(query);
        return iterator;
    }

    @VisibleForTesting
    EntityRecordStorage<I, S> entityRecordStorage() {
        return entityRecordStorage;
    }

    @VisibleForTesting
    MirrorStorage mirrorStorage() {
        return mirrorStorage;
    }
}
