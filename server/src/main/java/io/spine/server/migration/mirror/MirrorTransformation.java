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
import io.spine.protobuf.AnyPacker;
import io.spine.query.Column;
import io.spine.query.ColumnName;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.entity.EntityRecord;
import io.spine.server.entity.storage.EntityRecordSpec;
import io.spine.server.storage.RecordWithColumns;
import io.spine.system.server.Mirror;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;

/**
 * ...
 */
public class MirrorTransformation {

    protected EntityRecord transform(Mirror mirror) {
        var record = EntityRecord.newBuilder()
                .setEntityId(mirror.getId().getValue())
                .setState(mirror.getState())
                .setVersion(mirror.getVersion())
                .setLifecycleFlags(mirror.getLifecycle())
                .vBuild();
        return record;
    }

    @SuppressWarnings("unchecked")
    protected <I, S extends EntityState<I>, A extends Aggregate<I, S, ?>> RecordWithColumns<I, EntityRecord>
    transform(Class<A> aggregateClass, Mirror mirror) {
        var record = transform(mirror);
        var id = (I) record.getEntityId();

        var recordSpec = EntityRecordSpec.of(aggregateClass);
        var columns = fetchColumns(recordSpec, record);

        var result = RecordWithColumns.of(id, record, columns);
        return result;
    }

    private static Map<ColumnName, @Nullable Object>
    fetchColumns(EntityRecordSpec<?, ?, ?> recordSpec, EntityRecord record) {
        var result = recordSpec.columns()
                .stream()
                .collect(Collectors.toMap(
                        Column::name,
                        column -> extractColumnValue(column.name(), record)
                ));
        return unmodifiableMap(result);
    }

    private static Object extractColumnValue(ColumnName column, EntityRecord record) {
        var state = AnyPacker.unpack(record.getState());
        var descriptor = state.getDescriptorForType();
        var fieldDescriptor = descriptor.findFieldByName(column.value());
        var result = state.getField(fieldDescriptor);
        return result;
    }
}
