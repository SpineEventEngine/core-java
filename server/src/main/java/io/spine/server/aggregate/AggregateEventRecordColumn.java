/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.aggregate;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.server.entity.storage.ColumnName;
import io.spine.server.storage.RecordColumn;

/**
 * Columns stored along with an {@link AggregateEventRecord}.
 */
//TODO:2020-04-01:alex.tymchenko: consider introducing a common interface for the enums like this one.
public enum AggregateEventRecordColumn {

    aggregateId("aggregate_id",
                Any.class,
                AggregateEventRecord::getAggregateId),

    created("created",
            Timestamp.class,
            AggregateEventRecord::getTimestamp),

    version("version",
            Integer.class,
            r -> r.hasEvent() ? versionOfEvent(r)
                              : versionOfSnapshot(r)),

    snapshot("snapshot",
             Boolean.class,
             AggregateEventRecord::hasSnapshot);

    @SuppressWarnings("NonSerializableFieldInSerializableClass")
    private final RecordColumn<?, AggregateEventRecord> column;

    <T> AggregateEventRecordColumn(String columnName,
                                   Class<T> type,
                                   RecordColumn.Getter<AggregateEventRecord, T> getter) {
        ColumnName name = ColumnName.of(columnName);
        this.column = new RecordColumn<>(name, type, getter);
    }

    static ImmutableList<RecordColumn<?, AggregateEventRecord>> definitions() {
        ImmutableList.Builder<RecordColumn<?, AggregateEventRecord>> list = ImmutableList.builder();
        for (AggregateEventRecordColumn value : values()) {
            list.add(value.column);
        }
        return list.build();
    }

    RecordColumn<?, AggregateEventRecord> column() {
        return column;
    }

    private static int versionOfSnapshot(AggregateEventRecord rawRecord) {
        return rawRecord.getSnapshot()
                        .getVersion()
                        .getNumber();
    }

    private static int versionOfEvent(AggregateEventRecord rawRecord) {
        return rawRecord.getEvent()
                        .getContext()
                        .getVersion()
                        .getNumber();
    }
}
