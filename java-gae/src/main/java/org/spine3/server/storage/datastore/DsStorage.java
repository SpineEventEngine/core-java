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

import com.google.api.services.datastore.DatastoreV1.*;
import com.google.api.services.datastore.DatastoreV1.PropertyOrder.Direction;
import com.google.api.services.datastore.client.Datastore;
import com.google.api.services.datastore.client.DatastoreException;
import com.google.common.base.Function;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.TimestampOrBuilder;
import org.spine3.TypeName;
import org.spine3.server.storage.AggregateStorageRecord;
import org.spine3.server.storage.CommandStoreRecord;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static com.google.api.services.datastore.DatastoreV1.CommitRequest.Mode.NON_TRANSACTIONAL;
import static com.google.api.services.datastore.client.DatastoreHelper.*;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.protobuf.Descriptors.Descriptor;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.protobuf.Timestamps.convertToDate;

/**
 * Provides access to Google App Engine Cloud {@link Datastore}.
 *
 * @param <M> the type of messages to save to the storage.
 * @author Alexander Litus
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 */
class DsStorage<M extends Message> {

    private static final String VALUE_PROPERTY_NAME = "value";

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String TIMESTAMP_PROPERTY_NAME = "timestamp";

    private final Datastore datastore;
    private final TypeName typeName;

    /**
     * Creates a new storage instance.
     * @param descriptor the descriptor of the type of messages to save to the storage.
     * @param datastore the datastore implementation to use.
     */
    protected DsStorage(Descriptor descriptor, Datastore datastore) {
        this.datastore = datastore;
        this.typeName = TypeName.of(descriptor);
    }

    protected void storeWithAutoId(Property.Builder aggregateId, CommandStoreRecord record) {
        storeWithAutoId(aggregateId, record, record.getTimestamp());
    }

    protected void storeWithAutoId(Property.Builder aggregateId, AggregateStorageRecord record) {
        storeWithAutoId(aggregateId, record, record.getTimestamp());
    }

    private void storeWithAutoId(Property.Builder aggregateId, Message message, TimestampOrBuilder timestamp) {

        final Entity.Builder entity = messageToEntity(message, makeKey(typeName.nameOnly()));
        entity.addProperty(makeTimestampProperty(timestamp));
        entity.addProperty(aggregateId);

        final Mutation.Builder mutation = Mutation.newBuilder().addInsertAutoId(entity);
        commit(mutation);
    }

    protected void commit(Mutation.Builder mutation) {

        final CommitRequest commitRequest = CommitRequest.newBuilder()
                .setMode(NON_TRANSACTIONAL)
                .setMutation(mutation)
                .build();
        try {
            datastore.commit(commitRequest);
        } catch (DatastoreException e) {
            propagate(e);
        }
    }

    protected LookupResponse lookup(LookupRequest request) {
        LookupResponse response = null;
        try {
            response = datastore.lookup(request);
        } catch (DatastoreException e) {
            propagate(e);
        }
        return response;
    }

    protected List<M> runQuery(Query.Builder query) {

        final RunQueryRequest queryRequest = RunQueryRequest.newBuilder().setQuery(query).build();
        List<EntityResult> entityResults = newArrayList();

        try {
            entityResults = datastore.runQuery(queryRequest).getBatch().getEntityResultList();
        } catch (DatastoreException e) {
            propagate(e);
        }

        if (entityResults == null || entityResults.isEmpty()) {
            return newArrayList();
        }

        final List<M> result = transform(entityResults, entityToMessage);
        return result;
    }

    protected static Property.Builder makeTimestampProperty(TimestampOrBuilder timestamp) {
        final Date date = convertToDate(timestamp);
        return makeProperty(TIMESTAMP_PROPERTY_NAME, makeValue(date));
    }

    protected Query.Builder makeQuery(Direction sortDirection) {
        final Query.Builder query = Query.newBuilder();
        query.addKindBuilder().setName(typeName.nameOnly());
        query.addOrder(makeOrder(TIMESTAMP_PROPERTY_NAME, sortDirection));
        return query;
    }

    protected Key.Builder makeKindKey(String id) {
        return makeKey(typeName.nameOnly(), id);
    }

    protected static Entity.Builder messageToEntity(Message message, Key.Builder key) {
        final ByteString serializedMessage = toAny(message).getValue();
        final Entity.Builder entity = Entity.newBuilder()
                .setKey(key)
                .addProperty(makeProperty(VALUE_PROPERTY_NAME, makeValue(serializedMessage)));
        return entity;
    }

    protected M entityToMessage(@Nullable EntityResultOrBuilder entity) {

        if (entity == null) {
            @SuppressWarnings("unchecked") // cast is safe because Any is Message
            final M empty = (M) Any.getDefaultInstance();
            return empty;
        }

        final Any.Builder any = Any.newBuilder();

        final List<Property> properties = entity.getEntity().getPropertyList();

        for (Property property : properties) {
            if (property.getName().equals(VALUE_PROPERTY_NAME)) {
                any.setValue(property.getValue().getBlobValue());
            }
        }

        any.setTypeUrl(typeName.toTypeUrl());

        final M result = fromAny(any.build());
        return result;
    }

    private final Function<EntityResult, M> entityToMessage = new Function<EntityResult, M>() {
        @Override
        public M apply(@Nullable EntityResult entity) {
            return entityToMessage(entity);
        }
    };
}
