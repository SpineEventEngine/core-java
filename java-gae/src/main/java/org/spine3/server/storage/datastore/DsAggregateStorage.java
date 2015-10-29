/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage.datastore;

import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.AggregateStorageRecord;

import java.util.Iterator;
import java.util.List;

import static com.google.api.services.datastore.DatastoreV1.PropertyOrder.Direction.DESCENDING;
import static org.spine3.util.Identifiers.idToString;

/**
 * A storage of aggregate root events and snapshots based on Google Cloud Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsAggregateStorage<I> extends AggregateStorage<I> {

    private final DsDepository<AggregateStorageRecord> depository;

    private DsAggregateStorage(DsDepository<AggregateStorageRecord> depository) {
        this.depository = depository;
    }

    protected static <I> DsAggregateStorage<I> newInstance(DsDepository<AggregateStorageRecord> depository) {
        return new DsAggregateStorage<>(depository);
    }

    @Override
    protected void write(AggregateStorageRecord record) {
        depository.storeAggregateRecord(record.getAggregateId(), record);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {

        final String idString = idToString(id);
        final List<AggregateStorageRecord> records = depository.readByAggregateIdSortedByTime(idString, DESCENDING);
        return records.iterator();
    }

    @Override
    protected void releaseResources() {
        // NOP
    }
}
