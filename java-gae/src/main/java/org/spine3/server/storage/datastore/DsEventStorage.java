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

import com.google.api.services.datastore.DatastoreV1;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStoreRecord;

import java.util.Iterator;
import java.util.List;

import static com.google.api.services.datastore.DatastoreV1.PropertyOrder.Direction.ASCENDING;
import static org.spine3.util.Events.toEventRecordsIterator;

/**
 * Storage for event records based on Google Cloud Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsEventStorage extends EventStorage {

    private final DsStorage storage;

    private DsEventStorage(DsStorage storage) {
        this.storage = storage;
    }

    protected static DsEventStorage newInstance(DsStorage storage) {
        return new DsEventStorage(storage);
    }

    @Override
    public Iterator<EventRecord> allEvents() {

        final List<EventStoreRecord> records = readAllSortedByTime(ASCENDING);
        final Iterator<EventRecord> iterator = toEventRecordsIterator(records);
        return iterator;
    }

    @Override
    protected void write(EventStoreRecord record) {
        saveEvent(record.getEventId(), record);
    }

    @Override
    protected void releaseResources() {
        // NOP
    }

    /**
     * Stores the {@code record} by the {@code id}. Only one record could be stored by given id.
     */
    private void saveEvent(String id, EventStoreRecord record) {

        DatastoreV1.Entity.Builder entity = DsStorage.messageToEntity(record, storage.makeCommonKey(id));
        entity.addProperty(DsStorage.makeTimestampProperty(record.getTimestamp()));

        final DatastoreV1.Mutation.Builder mutation = DatastoreV1.Mutation.newBuilder().addInsert(entity);
        storage.commit(mutation);
    }

    /**
     * Reads all the elements.
     * @return the elements sorted by {@code timestamp} in specified {@code sortDirection}.
     */
    private List<EventStoreRecord> readAllSortedByTime(DatastoreV1.PropertyOrder.Direction sortDirection) {

        DatastoreV1.Query.Builder query = storage.makeQuery(sortDirection);
        @SuppressWarnings("unchecked") // TODO:2015-10-30:alexander.litus: avoid this
        final List<EventStoreRecord> records = (List<EventStoreRecord>) storage.runQuery(query);
        return records;
    }
}
