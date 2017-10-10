/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
 * A read request for {@link AggregateStorage}.
 *
 * <p>To organize efficient reading of an aggregate history, {@link AggregateStorage} should
 * read {@linkplain AggregateEventRecord aggregate event records} by batches.
 *
 * <p>The request specifies a size of the batches as a
 * {@linkplain #getSnapshotTrigger snapshot trigger} value.
 *
 * <p>Two requests for the aggregate with the same {@link #id} are considered equal.
 * {@link #snapshotTrigger} is not taken into account, because it should affect only
 * process of reading, but resulting records should be the same.
 *
 * @param <I> the type of the record ID
 * @author Dmytro Grankin
 */
@Internal
public final class AggregateReadRequest<I> implements ReadRequest<I> {

    private final I id;
    private final int snapshotTrigger;

    public AggregateReadRequest(I id, int snapshotTrigger) {
        checkArgument(snapshotTrigger > 0);
        this.id = checkNotNull(id);
        this.snapshotTrigger = snapshotTrigger;
    }

    @Override
    public I getId() {
        return id;
    }

    /**
     * Obtains the {@linkplain AggregateRepository#snapshotTrigger snapshot trigger}.
     *
     * <p>Use this value for the {@linkplain AggregateStorage#historyBackward(AggregateReadRequest)
     * history} reading optimization.
     *
     * <p>To load an aggregate, required only the last {@link Snapshot} and events occurred after
     * creation of this snapshot. So instead of reading all {@linkplain AggregateEventRecord
     * aggregate event records}, it is reasonable to read them by batches, size of which is equal
     * to the snapshot trigger value.
     *
     * <p>NOTE: A snapshot trigger can be changed for {@link AggregateRepository}
     * after the instantiation, so the amount of events between snapshots can be different
     * in an aggregate history and may look like this: {@code e1, s1, e2, e3, e4, s2, e5, e6, s3}.
     *
     * @return the snapshot trigger value
     */
    public int getSnapshotTrigger() {
        return snapshotTrigger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        AggregateReadRequest<?> that = (AggregateReadRequest<?>) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
