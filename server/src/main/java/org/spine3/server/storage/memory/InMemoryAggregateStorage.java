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

package org.spine3.server.storage.memory;

import org.spine3.server.aggregate.AggregateStorage;
import org.spine3.server.aggregate.storage.AggregateStatus;
import org.spine3.server.aggregate.storage.AggregateStorageRecord;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * In-memory storage for aggregate events and snapshots.
 *
 * @param <I> the type of IDs of aggregates managed by this storage
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
class InMemoryAggregateStorage<I> extends AggregateStorage<I> {

    private final MultitenantStorage<I, TenantAggregateRecords<I>> multitenantStorage;

    protected InMemoryAggregateStorage(boolean multitenant) {
        super(multitenant);
        this.multitenantStorage = new MultitenantStorage<I, TenantAggregateRecords<I>>(multitenant) {
            @Override
            TenantAggregateRecords<I> createSlice() {
                return new TenantAggregateRecords<>();
            }
        };
    }

    private TenantAggregateRecords<I> getStorage() {
        return multitenantStorage.getStorage();
    }

    /** Creates a new single-tenant storage instance. */
    protected static <I> InMemoryAggregateStorage<I> newInstance() {
        return new InMemoryAggregateStorage<>(false);
    }

    @Override
    protected void writeRecord(I id, AggregateStorageRecord record) {
        getStorage().put(id, record);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {
        checkNotNull(id);
        final List<AggregateStorageRecord> records = getStorage().getHistoryBackward(id);
        return records.iterator();
    }

    @Override
    protected int readEventCountAfterLastSnapshot(I id) {
        checkNotClosed();
        final int result = getStorage().getEventCount(id);
        return result;
    }

    @Override
    protected void writeEventCountAfterLastSnapshot(I id, int eventCount) {
        checkNotClosed();
        getStorage().putEventCount(id, eventCount);
    }

    @Override
    protected boolean markArchived(I id) {
        final AggregateStatus currentStatus = getStorage().getStatus(id);

        if (currentStatus.getArchived()) {
            return false; // Already archived.
        }

        getStorage().putStatus(id, currentStatus.toBuilder()
                                                .setArchived(true)
                                                .build());
        return true;
    }

    @Override
    protected boolean markDeleted(I id) {
        final AggregateStatus currentStatus = getStorage().getStatus(id);

        if (currentStatus.getDeleted()) {
            return false; // Already deleted.
        }

        getStorage().putStatus(id, currentStatus.toBuilder()
                                                .setDeleted(true)
                                                .build());
        return true;
    }
}
