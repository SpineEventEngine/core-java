/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.client.ArchivedColumn;
import io.spine.client.DeletedColumn;
import io.spine.client.VersionColumn;
import io.spine.core.Version;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.query.CustomColumn;
import io.spine.query.RecordColumn;
import io.spine.query.RecordColumns;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.WithLifecycle;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

/**
 * //TODO:2020-12-15:alex.tymchenko: update the description.
 * Columns storing the internal framework-specific attributes of an {@code Entity}.
 *
 * <p>As they are not declared in the Proto messages of entity states, they are implemented as
 * several {@linkplain CustomColumn custom columns}. Their actual values are obtained from
 * the entity attributes at the very moment of persisting a corresponding {@code Entity} instance.
 */
@Internal
@RecordColumns(ofType = EntityRecord.class)
final class EntityRecordColumn {

    static final RecordColumn<EntityRecord, Boolean> archived =
            AsEntityRecordColumn.apply(ArchivedColumn.instance(),
                                       Boolean.class,
                                       WithLifecycle::isArchived);

    static final RecordColumn<EntityRecord, Boolean> deleted =
            AsEntityRecordColumn.apply(DeletedColumn.instance(),
                                       Boolean.class,
                                       WithLifecycle::isDeleted);

    static final RecordColumn<EntityRecord, Version> version =
            AsEntityRecordColumn.apply(VersionColumn.instance(),
                                       Version.class,
                                       EntityRecord::getVersion);

    private static final ImmutableSet<RecordColumn<EntityRecord, ?>> all =
            ImmutableSet.of(archived, deleted, version);

    /**
     * Prevents instantiation of this column holder type.
     */
    private EntityRecordColumn() {
    }

    /**
     * Returns the names of all columns.
     */
    @VisibleForTesting
    static ImmutableSet<ColumnName> names() {
        return all.stream()
                  .map(Column::name)
                  .collect(toImmutableSet());
    }

    static ImmutableMap<ColumnName, Object> valuesIn(EntityRecord record) {
        checkNotNull(record);
        ImmutableMap<ColumnName, Object> values =
                all.stream()
                   .collect(toImmutableMap(Column::name, c -> c.valueIn(record)));
        return values;
    }
}
