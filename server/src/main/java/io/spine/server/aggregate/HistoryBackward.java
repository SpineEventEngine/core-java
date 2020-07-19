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

import com.google.protobuf.Any;
import io.spine.annotation.SPI;
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

import static io.spine.query.Direction.DESC;
import static io.spine.server.aggregate.AggregateEventRecordColumn.aggregate_id;
import static io.spine.server.aggregate.AggregateEventRecordColumn.created;
import static io.spine.server.aggregate.AggregateEventRecordColumn.version;

/**
 * Reads the history of an {@link Aggregate} in a chronological order.
 */
@SPI
public class HistoryBackward<I> {

    private final AggregateEventStorage eventStorage;

    protected HistoryBackward(AggregateEventStorage storage) {
        eventStorage = storage;
    }

    protected Iterator<AggregateEventRecord>
    read(I aggregateId, int batchSize, @Nullable Version startingFrom) {
        RecordQueryBuilder<AggregateEventRecordId, AggregateEventRecord> builder =
                historyBackwardQuery(aggregateId);
        if (startingFrom != null) {
            builder.where(version)
                   .isLessThan(startingFrom.getNumber());
        }
        RecordQuery<AggregateEventRecordId, AggregateEventRecord> query =
                inChronologicalOrder(builder, batchSize).build();
        Iterator<AggregateEventRecord> iterator = eventStorage.readAll(query);
        return iterator;
    }

    private RecordQueryBuilder<AggregateEventRecordId, AggregateEventRecord>
    historyBackwardQuery(I id) {
        Any packedId = Identifier.pack(id);
        return eventStorage.queryBuilder()
                           .where(aggregate_id)
                           .is(packedId);
    }

    static <B extends RecordQueryBuilder<AggregateEventRecordId, AggregateEventRecord>> B
    inChronologicalOrder(B builder, @Nullable Integer batchSize) {
        builder.orderBy(version, DESC)
               .orderBy(created, DESC);
        if(batchSize != null) {
            builder.limit(batchSize);
        }
        return builder;
    }
}
