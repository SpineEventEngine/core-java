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

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

import static com.google.api.services.datastore.DatastoreV1.CommitRequest.Mode.NON_TRANSACTIONAL;
import static com.google.api.services.datastore.client.DatastoreHelper.*;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static org.spine3.protobuf.Messages.fromAny;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.protobuf.Timestamps.convertToDate;

/**
 * The Google App Engine Cloud {@link Datastore} wrapper.
 *
 * @author Alexander Litus
 * @see DatastoreStorageFactory
 * @see LocalDatastoreStorageFactory
 */
class DatastoreWrapper {

    private static final String VALUE_PROPERTY_NAME = "value";

    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static final String TIMESTAMP_PROPERTY_NAME = "timestamp";

    private final Datastore datastore;

    /**
     * Creates a new storage instance.
     *
     * @param datastore the datastore implementation to use.
     */
    protected DatastoreWrapper(Datastore datastore) {
        this.datastore = datastore;
    }

    /**
     * Commits the given mutation.
     *
     * @param mutation the mutation to commit.
     * @throws RuntimeException if {@link Datastore#commit(CommitRequest)} throws a {@link DatastoreException}
     */
    protected void commit(Mutation mutation) {

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

    /**
     * Commits the given mutation.
     *
     * @param mutation the mutation to commit.
     * @throws RuntimeException if {@link Datastore#commit(CommitRequest)} throws a {@link DatastoreException}
     */
    @SuppressWarnings("TypeMayBeWeakened") // no, it cannot
    protected void commit(Mutation.Builder mutation) {
        commit(mutation.build());
    }

    /**
     * Commits the {@link LookupRequest}.
     *
     * @param request the request to commit
     * @return the {@link LookupResponse} received
     * @throws RuntimeException if {@link Datastore#lookup(LookupRequest)} throws a {@link DatastoreException}
     */
    protected LookupResponse lookup(LookupRequest request) {
        LookupResponse response = null;
        try {
            response = datastore.lookup(request);
        } catch (DatastoreException e) {
            propagate(e);
        }
        return response;
    }

    /**
     * Runs the given {@link Query}.
     *
     * @param query the query to run.
     * @return the {@link EntityResult} list received or an empty list
     * @throws RuntimeException if {@link Datastore#lookup(LookupRequest)} throws a {@link DatastoreException}
     */
    protected List<EntityResult> runQuery(Query query) {
        final RunQueryRequest queryRequest = RunQueryRequest.newBuilder().setQuery(query).build();
        return runQueryRequest(queryRequest);
    }

    /**
     * Runs the {@link Query} got from the given builder.
     *
     * @param query the query builder to build and run.
     * @return the {@link EntityResult} list received or an empty list
     * @throws RuntimeException if {@link Datastore#lookup(LookupRequest)} throws a {@link DatastoreException}
     */
    @SuppressWarnings("TypeMayBeWeakened") // no, it cannot
    protected List<EntityResult> runQuery(Query.Builder query) {
        return runQuery(query.build());
    }

    private List<EntityResult> runQueryRequest(RunQueryRequest queryRequest) {

        List<EntityResult> entityResults = newArrayList();
        try {
            entityResults = datastore.runQuery(queryRequest).getBatch().getEntityResultList();
        } catch (DatastoreException e) {
            propagate(e);
        }
        if (entityResults == null) {
            entityResults = newArrayList();
        }
        return entityResults;
    }

    /**
     * Makes a property from the given timestamp using {@link org.spine3.protobuf.Timestamps#convertToDate(TimestampOrBuilder)}.
     */
    protected static Property.Builder makeTimestampProperty(TimestampOrBuilder timestamp) {
        final Date date = convertToDate(timestamp);
        return makeProperty(TIMESTAMP_PROPERTY_NAME, makeValue(date));
    }

    /**
     * Builds a query with the given {@code Entity} kind and the {@code Timestamp} sort direction.
     *
     * @param sortDirection the {@code Timestamp} sort direction
     * @param entityKind    the {@code Entity} kind
     * @return a new {@code Query} instance.
     * @see Entity
     * @see com.google.protobuf.Timestamp
     * @see Query
     */
    protected static Query.Builder makeQuery(Direction sortDirection, String entityKind) {
        final Query.Builder query = Query.newBuilder();
        query.addKindBuilder().setName(entityKind);
        query.addOrder(makeOrder(TIMESTAMP_PROPERTY_NAME, sortDirection));
        return query;
    }

    /**
     * Converts the given {@link Message} to the {@link Entity.Builder}.
     *
     * @param message the message to convert
     * @param key the entity key to set
     * @return the {@link Entity.Builder} with the given key and property created from the serialized message
     */
    protected static Entity.Builder messageToEntity(Message message, Key.Builder key) {
        final ByteString serializedMessage = toAny(message).getValue();
        final Entity.Builder entity = Entity.newBuilder()
                .setKey(key)
                .addProperty(makeProperty(VALUE_PROPERTY_NAME, makeValue(serializedMessage)));
        return entity;
    }

    /**
     * Converts the given {@link EntityResultOrBuilder} to the {@link Message}.
     *
     * @param entity the entity to convert
     * @param typeUrl the type url of the message
     * @return the deserialized message
     * @see Any#getTypeUrl()
     */
    protected static <M extends Message> M entityToMessage(@Nullable EntityResultOrBuilder entity, String typeUrl) {

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

        any.setTypeUrl(typeUrl);

        final M result = fromAny(any.build());
        return result;
    }

    /**
     * Converts the given {@link EntityResult} list to the {@link Message} list.
     *
     * @param entities the entities to convert
     * @param typeUrl the type url of the messages
     * @return the deserialized messages
     * @see Any#getTypeUrl()
     */
    protected static <M extends Message> List<M> entitiesToMessages(List<EntityResult> entities, final String typeUrl) {

        final Function<EntityResult, M> entityToMessage = new Function<EntityResult, M>() {
            @Override
            public M apply(@Nullable EntityResult entity) {
                return entityToMessage(entity, typeUrl);
            }
        };
        final List<M> messages = transform(entities, entityToMessage);
        return messages;
    }
}
