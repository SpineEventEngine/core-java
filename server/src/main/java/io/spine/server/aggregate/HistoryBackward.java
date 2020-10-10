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
import io.spine.base.Identifier;
import io.spine.core.Version;
import io.spine.query.RecordQuery;
import io.spine.query.RecordQueryBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.aggregate.AggregateEventRecordColumn.aggregate_id;
import static io.spine.server.aggregate.AggregateEventRecordColumn.created;
import static io.spine.server.aggregate.AggregateEventRecordColumn.version;

/**
 * Reads the history of an {@link Aggregate} in a chronological order.
 *
 * @param <I> the type of the identifiers of the stored aggregates
 */
final class HistoryBackward<I> {

    private final AggregateEventStorage eventStorage;

    /**
     * Creates an instance of this read operation for the given storage of the historical
     * aggregate events.
     */
    HistoryBackward(AggregateEventStorage storage) {
        eventStorage = storage;
    }

    /**
     * Reads the history.
     *
     * @param aggregateId
     *         the identifier of the aggregate to read the history for
     * @param batchSize
     *         the maximum number of the records to read per a single query to the storage
     * @param startingFrom
     *         the version of the event to start from, exclusive;
     *         this parameter is optional, end-users should pass {@code null}
     *         if no such a conditional restriction is required
     */
    Iterator<AggregateEventRecord>
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

    /**
     * Adds the criteria to the passed query builder making the query results to be sorted
     * in a chronological order.
     *
     * <p>Optionally, allows to limit the number of returned event records to some number.
     *
     * @param builder
     *         the query builder to append the chronological sorting to
     * @param limit
     *         maximum size of the event records returned; optional, end-users should pass
     *         {@code null} if the limiting the query results to some size isn't required
     * @param <B>
     *         the type of the query builder
     * @return the query builder with the chronological criteria and, optionally, limit appended
     */
    static <B extends RecordQueryBuilder<AggregateEventRecordId, AggregateEventRecord>> B
    inChronologicalOrder(B builder, @Nullable Integer limit) {
        checkNotNull(builder);
        builder.sortDescendingBy(version)
               .sortDescendingBy(created);
        if (limit != null) {
            builder.limit(limit);
        }
        return builder;
    }
}
