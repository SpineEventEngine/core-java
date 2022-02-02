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

import io.spine.base.EntityState;
import io.spine.server.ContextSpec;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.system.server.Mirror;

/**
 * Migrates {@linkplain Mirror} projections into
 * {@linkplain io.spine.server.entity.storage.EntityRecordWithColumns EntityRecordWithColumns}.
 *
 * <p>{@code Mirror} was deprecated in Spine 2.x. Now, {@code EntityRecordWithColumns} is used
 * to store the aggregate's state for further querying.
 */
public final class MirrorMigration {

    private final MirrorStorage mirrors;
    private final MirrorMapping mapping;
    private final StorageFactory storageFactory;
    private final ContextSpec contextSpec;

    public MirrorMigration(ContextSpec contextSpec, StorageFactory storageFactory) {
        this(new MirrorMapping.Default(), contextSpec, storageFactory);
    }

    public MirrorMigration(
            MirrorMapping mapping,
            ContextSpec contextSpec,
            StorageFactory storageFactory
    ) {
        this.mirrors = new MirrorStorage(contextSpec, storageFactory);
        this.mapping = mapping;
        this.storageFactory = storageFactory;
        this.contextSpec = contextSpec;
    }

    /**
     * Migrates {@linkplain Mirror} projections which belong to the specified aggregate
     * to the {@linkplain EntityRecordStorage} of that aggregate.
     *
     * @param aggregateClass
     *         the type of aggregate, mirror projections of which are to be migrated
     * @param <I>
     *         the aggregate's identifier
     * @param <S>
     *         the aggregate's state
     * @param <A>
     *         the aggregate's class
     */
    public <I, S extends EntityState<I>, A extends Aggregate<I, S, ?>> void
    run(Class<A> aggregateClass, MigrationSupervisor supervisor) {

        supervisor.onMigrationStarted();

        var completedStep = proceed(supervisor);
        while (completedStep.getMigrated() == supervisor.stepSize()) {
            completedStep = proceed(supervisor);
        }

        supervisor.onMigrationCompleted();
    }

    private MigrationStep proceed(MigrationSupervisor supervisor) {

        supervisor.onStepStarted();

        var entityRecords = storageFactory
                .createEntityRecordStorage(contextSpec, aggregateClass);
        var aggregateType = AggregateClass.asAggregateClass(aggregateClass)
                                          .stateTypeUrl()
                                          .value();
        var aggregateMirrors = mirrors.queryBuilder()
                                      .where(Mirror.Column.aggregateType())
                                      .is(aggregateType)
                                      .limit(supervisor.stepSize())
                                      .build();
        mirrors.readAll(aggregateMirrors)
               .forEachRemaining(mirror -> {
                   var recordWithColumns = mapping.toRecordWithColumns(mirror, aggregateClass);
                   entityRecords.write(recordWithColumns);
               });

        var completedStep = MigrationStep.newBuilder()
                .setMigrated(123)
                .build();

        supervisor.onStepCompleted(completedStep);

        return completedStep;
    }
}
