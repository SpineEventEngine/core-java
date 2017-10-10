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

import io.spine.annotation.SPI;
import io.spine.server.storage.ReadRequest;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A read request for {@link AggregateStorage}.
 *
 * @param <I> the type of the target ID
 * @author dmytro.grankin
 */
@SPI
public final class AggregateReadRequest<I> implements ReadRequest<I> {

    private final I id;
    private final int snapshotTrigger;

    public AggregateReadRequest(I id, int snapshotTrigger) {
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
     * @return the snapshot trigger value
     */
    @SuppressWarnings("unused")
    public int getSnapshotTrigger() {
        return snapshotTrigger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        AggregateReadRequest<?> that = (AggregateReadRequest<?>) o;

        return snapshotTrigger == that.snapshotTrigger && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
