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

import com.google.common.collect.Lists;
import io.spine.base.EntityState;
import io.spine.client.ArchivedColumn;
import io.spine.client.DeletedColumn;
import io.spine.query.RecordColumn;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordStorage;

import java.util.Collection;

class ExposedEntityRecordStorage<I, S extends EntityState<I>> {

    private final RecordColumn<EntityRecord, Boolean> archived =
            RecordColumn.create(
                    ArchivedColumn.instance().name().value(),
                    Boolean.class,
                    EntityRecord::isArchived
            );
    private final RecordColumn<EntityRecord, Boolean> deleted =
            RecordColumn.create(
                    DeletedColumn.instance().name().value(),
                    Boolean.class,
                    EntityRecord::isDeleted
            );
    private final EntityRecordStorage<I, S> storage;

    ExposedEntityRecordStorage(EntityRecordStorage<I, S> storage) {
        this.storage = storage;
    }

    Collection<EntityRecord> readActive() {
        var iterator = storage.readAll();
        var active = Lists.newArrayList(iterator);
        return active;
    }

    Collection<EntityRecord> readArchived() {
        var query = storage.queryBuilder()
                .where(archived).is(true)
                .build();
        var iterator = storage.readAll(query);
        var archived = Lists.newArrayList(iterator);
        return archived;
    }

    Collection<EntityRecord> readDeleted() {
        var query = storage.queryBuilder()
                .where(deleted).is(true)
                .build();
        var iterator = storage.readAll(query);
        var archived = Lists.newArrayList(iterator);
        return archived;
    }
}
