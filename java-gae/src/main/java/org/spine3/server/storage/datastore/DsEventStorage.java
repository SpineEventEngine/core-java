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

import org.spine3.TypeName;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStoreRecord;

import java.util.Iterator;
import java.util.List;

import static com.google.api.services.datastore.DatastoreV1.*;
import static com.google.api.services.datastore.DatastoreV1.PropertyOrder.Direction.ASCENDING;
import static com.google.api.services.datastore.client.DatastoreHelper.makeKey;
import static org.spine3.server.storage.datastore.DatastoreWrapper.*;
import static org.spine3.util.Events.toEventRecordsIterator;

/**
 * Storage for event records based on Google Cloud Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsEventStorage extends EventStorage {

    private final DatastoreWrapper datastore;
    private static final String KIND = EventStoreRecord.class.getName();

    protected static DsEventStorage newInstance(DatastoreWrapper datastore) {
        return new DsEventStorage(datastore);
    }

    private DsEventStorage(DatastoreWrapper datastore) {
        this.datastore = datastore;
    }

    @Override
    public Iterator<EventRecord> allEvents() {

        final Query.Builder query = makeQuery(ASCENDING, KIND);
        // TODO:2015-10-30:alexander.litus: change
        final String typeUrl = TypeName.of(EventStoreRecord.getDescriptor()).toTypeUrl();
        final List<EntityResult> entityResults = datastore.runQuery(query);
        final List<EventStoreRecord> records = entitiesToMessages(entityResults, typeUrl);
        final Iterator<EventRecord> iterator = toEventRecordsIterator(records);
        return iterator;
    }

    @Override
    protected void write(EventStoreRecord record) {

        final Key.Builder key = makeKey(KIND, record.getEventId());
        final Entity.Builder entity = messageToEntity(record, key);
        entity.addProperty(makeTimestampProperty(record.getTimestamp()));

        final Mutation.Builder mutation = Mutation.newBuilder().addInsert(entity);
        datastore.commit(mutation);
    }

    @Override
    protected void releaseResources() {
        // NOP
    }
}
