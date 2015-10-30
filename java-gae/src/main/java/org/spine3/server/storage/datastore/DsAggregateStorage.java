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

import static com.google.api.services.datastore.DatastoreV1.*;
import static com.google.api.services.datastore.DatastoreV1.PropertyFilter.Operator.EQUAL;
import static com.google.api.services.datastore.DatastoreV1.PropertyOrder.Direction.DESCENDING;
import static com.google.api.services.datastore.client.DatastoreHelper.*;
import static org.spine3.TypeName.toTypeUrl;
import static org.spine3.server.storage.datastore.DatastoreWrapper.*;
import static org.spine3.util.Identifiers.idToString;

/**
 * A storage of aggregate root events and snapshots based on Google Cloud Datastore.
 *
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 * @author Alexander Litus
 */
class DsAggregateStorage<I> extends AggregateStorage<I> {

    private static final String AGGREGATE_ID_PROPERTY_NAME = "aggregateId";
    private static final String KIND = AggregateStorageRecord.class.getName();
    private static final String TYPE_URL = toTypeUrl(AggregateStorageRecord.getDescriptor());

    private final DatastoreWrapper datastore;

    protected static <I> DsAggregateStorage<I> newInstance(DatastoreWrapper datastore) {
        return new DsAggregateStorage<>(datastore);
    }

    private DsAggregateStorage(DatastoreWrapper datastore) {
        this.datastore = datastore;
    }

    @Override
    protected void write(AggregateStorageRecord record) {

        final Value.Builder idValue = makeValue(record.getAggregateId());
        final Property.Builder idProperty = makeProperty(AGGREGATE_ID_PROPERTY_NAME, idValue);
        final Entity.Builder entity = messageToEntity(record, makeKey(KIND));
        entity.addProperty(idProperty);
        entity.addProperty(makeTimestampProperty(record.getTimestamp()));

        final Mutation.Builder mutation = Mutation.newBuilder().addInsertAutoId(entity);
        datastore.commit(mutation);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {

        final String idString = idToString(id);
        final Filter.Builder idFilter = makeFilter(AGGREGATE_ID_PROPERTY_NAME, EQUAL, makeValue(idString));
        final Query.Builder query = makeQuery(DESCENDING, KIND);
        query.setFilter(idFilter).build();

        final List<EntityResult> entityResults = datastore.runQuery(query);
        final List<AggregateStorageRecord> records = entitiesToMessages(entityResults, TYPE_URL);
        return records.iterator();
    }

    @Override
    protected void releaseResources() {
        // NOP
    }
}
