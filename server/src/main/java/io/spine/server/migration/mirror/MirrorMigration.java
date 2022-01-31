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
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.model.AggregateClass;
import io.spine.server.entity.storage.EntityRecordStorage;
import io.spine.server.storage.RecordStorage;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

public final class MirrorMigration {

    private final RecordStorage<MirrorId, Mirror> mirrors;
    private final MirrorMapping mapping;

    public MirrorMigration(RecordStorage<MirrorId, Mirror> mirrors) {
        this(mirrors, new MirrorMapping.Default());
    }

    public MirrorMigration(RecordStorage<MirrorId, Mirror> mirrors, MirrorMapping mapping) {
        this.mirrors = mirrors;
        this.mapping = mapping;
    }

    /**
     * Migrates {@linkplain Mirror} projections which belong to the specified aggregate
     * to the {@linkplain EntityRecordStorage} of that aggregate.
     *
     * @param aggregateClass
     *         the type of aggregate, mirror projections of which are to be migrated
     * @param entityRecords
     *         the entity records storage of the aggregate
     * @param <I>
     *         the aggregate's identifier
     * @param <S>
     *         the aggregate's state
     * @param <A>
     *         the aggregate's class
     */
    public <I, S extends EntityState<I>, A extends Aggregate<I, S, ?>> void
    run(Class<A> aggregateClass, EntityRecordStorage<I, S> entityRecords) {
        var aggregateType = AggregateClass.asAggregateClass(aggregateClass)
                                          .stateTypeUrl()
                                          .value();
        var query = mirrors.queryBuilder().build();
        mirrors.readAll(query)
               .forEachRemaining(mirror -> {

                   if (!mirror.getAggregateType().equals(aggregateType)) {
                       return;
                   }

                   var recordWithColumns = mapping.toRecordWithColumns(aggregateClass, mirror);
                   entityRecords.write(recordWithColumns);
               });
    }
}
