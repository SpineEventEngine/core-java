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
import io.spine.client.OrderBy;
import io.spine.client.ResponseFormat;
import io.spine.core.Version;
import io.spine.server.storage.QueryParameters;
import io.spine.server.storage.RecordQueries;
import io.spine.server.storage.RecordQuery;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

import static io.spine.server.aggregate.AggregateEventRecordColumn.aggregate_id;
import static io.spine.server.aggregate.AggregateEventRecordColumn.created;
import static io.spine.server.aggregate.AggregateEventRecordColumn.version;

/**
 * Reads the history of an {@link Aggregate} in a chronological order.
 */
@SPI
public class HistoryBackward<I> {

    private static final OrderBy newestFirst = newestFirst();
    private static final OrderBy higherVersionFirst = higherVersionFirst();

    private final AggregateEventStorage eventStorage;

    protected HistoryBackward(AggregateEventStorage storage) {
        eventStorage = storage;
    }

    protected Iterator<AggregateEventRecord>
    read(I aggregateId, int batchSize, @Nullable Version startingFrom) {
        RecordQuery<AggregateEventRecordId> query = historyBackwardQuery(aggregateId);
        if (startingFrom != null) {
            QueryParameters lessThanVersion = QueryParameters.lt(version, startingFrom.getNumber());
            query = query.append(lessThanVersion);
        }
        ResponseFormat responseFormat = chronologicalResponseWith(batchSize);
        Iterator<AggregateEventRecord> iterator = eventStorage.readAll(query, responseFormat);
        return iterator;
    }

    protected RecordQuery<AggregateEventRecordId> historyBackwardQuery(I id) {
        Any packedId = Identifier.pack(id);
        QueryParameters params = QueryParameters.eq(aggregate_id, packedId);
        return RecordQueries.of(params);
    }

    protected static ResponseFormat chronologicalResponseWith(@Nullable Integer batchSize) {
        ResponseFormat.Builder builder =
                ResponseFormat.newBuilder()
                              .addOrderBy(higherVersionFirst)
                              .addOrderBy(newestFirst);
        if (batchSize != null) {
            builder.setLimit(batchSize);
        }
        return builder.vBuild();
    }

    private static OrderBy newestFirst() {
        return OrderBy.newBuilder()
                      .setDirection(OrderBy.Direction.DESCENDING)
                      .setColumn(created.name())
                      .vBuild();
    }

    private static OrderBy higherVersionFirst() {
        return OrderBy.newBuilder()
                      .setDirection(OrderBy.Direction.DESCENDING)
                      .setColumn(version.name())
                      .vBuild();
    }
}
