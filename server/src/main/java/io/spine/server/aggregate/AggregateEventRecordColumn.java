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
import io.spine.query.Column;
import io.spine.query.RecordColumn;

/**
 * Columns stored along with an {@link AggregateEventRecord}.
 */
@SuppressWarnings("DuplicateStringLiteralInspection")   // column names repeat in different records.
public final class AggregateEventRecordColumn {

    static final RecordColumn<AggregateEventRecord, Any> aggregate_id =
            new RecordColumn<>("aggregate_id", Any.class, AggregateEventRecord::getAggregateId);

    static final RecordColumn<AggregateEventRecord, Timestamp> created =
            new RecordColumn<>("created", Timestamp.class, AggregateEventRecord::getTimestamp);

    static final RecordColumn<AggregateEventRecord, Integer> version =
            new RecordColumn<>("version", Integer.class, versionGetter());

    static final RecordColumn<AggregateEventRecord, Boolean> snapshot =
            new RecordColumn<>("snapshot", Boolean.class, AggregateEventRecord::hasSnapshot);

    private AggregateEventRecordColumn() {
    }

    static ImmutableList<RecordColumn<AggregateEventRecord, ?>> definitions() {
        return ImmutableList.of(aggregate_id, created, version, snapshot);
    }

    private static Column.Getter<AggregateEventRecord, Integer> versionGetter() {
        return r -> r.hasEvent() ? versionOfEvent(r)
                                 : versionOfSnapshot(r);
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
