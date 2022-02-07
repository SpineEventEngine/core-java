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
 * Migrates {@linkplain Mirror} projections into {@linkplain EntityRecordWithColumns}.
 *
 * <p>{@code Mirror} projection was deprecated in Spine 2.x. Previously, it was used to store
 * aggregates' states to allow their querying. A single projection stored states of <b>all</b>
 * aggregates in a Bounded Context.
 *
 * <p>Now, {@linkplain EntityRecordStorage} is used to store the aggregate's states when it is
 * open for querying. This storage is created on a <b>per-aggregate</b> basis. This enables
 * storing of queryable state-based columns along the state itself. For this reason, the migration
 * is also done on a per-aggregate basis.
 *
 * <p><b>An example usage</b>
 *
 * <p>1. Create an instance of `MirrorMigration`:
 * <pre>
 * var contextSpec = ContextSpec.singleTenant("...");
 * var storageFactory = ServerEnvironment.instance().storageFactory();
 * var migration = new MirrorMigration(contextSpec, storageFactory, ParcelAgg.class);
 * </pre>
 *
 * <p>2. Create `MigrationSupervisor`. Here we are going to use a default implementation,
 * which always continues the migration. Also, we will work with storages via batches
 * of 500 records.
 * <pre>
 * var stepSize = 500;
 * var supervisor = new MigrationSupervisor(stepSize);
 * </pre>
 *
 * <p>3. Run the migration.
 * <pre>
 * migration.run(supervisor);
 * </pre>
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

    /**
     * Creates a new instance of {@code MirrorMigration}.
     *
     * <p>The created instance is able to migrate {@code Mirror} projections of the
     * given aggregate class.
     *
     * @param contextSpec
     *         the context to which the passed aggregate belongs
     * @param factory
     *         the storage provider
     * @param aggClass
     *         the class of an aggregate, {@code Mirror} projections of which are to be migrated
     */
    public MirrorMigration(ContextSpec contextSpec,
                           StorageFactory factory,
                           Class<A> aggClass) {

        this.mirrorStorage = new MirrorStorage(contextSpec, factory);
        this.entityRecordStorage = factory.createEntityRecordStorage(contextSpec, aggClass);
        this.mapping = new MirrorMapping<>(aggClass);
        this.aggregateType = AggregateClass.asAggregateClass(aggClass)
                                          .stateTypeUrl()
                                          .value();
    }

    /**
     * Migrates {@linkplain Mirror} projections
     * to the aggregate's {@linkplain EntityRecordStorage}.
     */
    public void run(MigrationSupervisor supervisor) {
        supervisor.onMigrationStarted();

        var step = proceed(supervisor);
        while (step.getMigrated() != 0 && supervisor.shouldContinueAfter(step)) {
            step = proceed(supervisor);
        }

        supervisor.onMigrationCompleted();
    }

    /**
     * Fetches and processes the next batch.
     */
    private MigrationStep proceed(MigrationSupervisor supervisor) {
        supervisor.onStepStarted();

        var batchSize = supervisor.stepSize();
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
