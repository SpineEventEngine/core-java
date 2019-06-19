/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import io.spine.annotation.Internal;
import io.spine.server.storage.ReadRequest;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A request to read events for a particular {@code Aggregate} from {@link AggregateStorage}.
 *
 * <p>A result of processing this request is a {@linkplain AggregateHistory record},
 * which satisfies the request criteria.
 *
 * <p>In addition to a record identifier, this request implementation requires
 * the batch size for event reading to be set. Typically that would be a number of
 * event records to read to encounter the most recent snapshot.
 *
 * <p>Two requests with the same {@linkplain #recordId() record ID} are considered equal.
 * {@linkplain #batchSize() Batch size} is not taken into account, because it should affect only
 * process of reading, but resulting records should be the same.
 *
 * @param <I> the type of the record ID
 */
@Internal
public final class AggregateReadRequest<I> implements ReadRequest<I> {

    private final I recordId;
    private final int batchSize;

    public AggregateReadRequest(I recordId, int batchSize) {
        checkArgument(batchSize > 0);
        this.recordId = checkNotNull(recordId);
        this.batchSize = batchSize;
    }

    @Override
    public I recordId() {
        return recordId;
    }

    /**
     * Obtains the number of {@linkplain AggregateEventRecord events} to read per batch.
     *
     * @return the events batch size
     */
    public int batchSize() {
        return batchSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        AggregateReadRequest<?> that = (AggregateReadRequest<?>) o;

        return recordId.equals(that.recordId);
    }

    @Override
    public int hashCode() {
        return recordId.hashCode();
    }
}
