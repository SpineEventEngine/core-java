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

package org.spine3.gae.datastore;

import com.google.appengine.api.datastore.*;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.engine.AbstractStorage;
import org.spine3.util.JsonFormat;
import org.spine3.util.Messages;

import java.util.ArrayList;
import java.util.List;

import static com.google.appengine.api.datastore.Query.FilterOperator.*;

/**
 * @author Mikhail Mikhaylov.
 */
public class DataStoreStorage extends AbstractStorage {

    public static final String VALUE_KEY = "value";
    public static final String TYPE_URL_KEY = "type_url";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String TIMESTAMP_NANOS_KEY = "timestamp_nanos";
    public static final String VERSION_KEY = "version";
    public static final String AGGREGATE_ID_KEY = "aggregate_id";
    public static final String SINGLETON_ID_KEY = "singleton_id";

    private static final String SINGLETON_KIND = "singleton";

    private final DatastoreService dataStore;

    public DataStoreStorage() {
        dataStore = DatastoreServiceFactory.getDatastoreService();
    }

    @Override
    public void store(Message message) {
        final Entity dataStoreEntity = Converters.convert(message);

        dataStore.put(dataStoreEntity);
    }

    @Override
    public void storeSingleton(Message id, Message message) {
        final Entity dataStoreEntity = new Entity(SINGLETON_KIND, JsonFormat.printToString(id));

        final Any any = Messages.toAny(message);

        dataStoreEntity.setProperty(VALUE_KEY, new Blob(any.getValue().toByteArray()));
        dataStoreEntity.setProperty(TYPE_URL_KEY, any.getTypeUrl());
        dataStoreEntity.setProperty(SINGLETON_ID_KEY, JsonFormat.printToString(id));

        dataStore.put(dataStoreEntity);
    }

    @Override
    public <T extends Message> T readSingleton(Message id) {
        return readMessageFromDataStore(SINGLETON_KIND, JsonFormat.printToString(id));
    }

    @Override
    public <T extends Message> List<T> query(Class clazz) {
        return readMessagesFromDataStore(clazz.getName());
    }

    @Override
    public <T extends Message> List<T> query(Class clazz, Message aggregateRootId, int version) {
        return readMessagesFromDataStore(clazz.getName(),
                prepareAggregateRootIdAndVersionFilter(aggregateRootId, version));
    }

    @Override
    public <T extends Message> List<T> query(Class clazz, Message aggregateRootId) {
        return readMessagesFromDataStore(clazz.getName(), new Query.FilterPredicate(
                AGGREGATE_ID_KEY, EQUAL, JsonFormat.printToString(aggregateRootId)));
    }

    @Override
    public <T extends Message> List<T> query(Class clazz, Message aggregateRootId, Timestamp from) {
        return readMessagesFromDataStore(clazz.getName(), new Query.FilterPredicate(
                AGGREGATE_ID_KEY, EQUAL, prepareAggregateRootIdAndTimestampFilter(aggregateRootId, from)));
    }

    @Override
    public <T extends Message> List<T> query(Class clazz, Timestamp from) {
        return readMessagesFromDataStore(clazz.getName(), new Query.FilterPredicate(
                AGGREGATE_ID_KEY, EQUAL, prepareTimestampFilter(from)));
    }

    private <T extends Message> List<T> readMessagesFromDataStore(String kind) {
        return readMessagesFromDataStore(kind, null);
    }

    private <T extends Message> List<T> readMessagesFromDataStore(String kind, Query.Filter filter) {
        final Query query = new Query(kind);
        if (filter != null) {
            query.setFilter(filter);
        }

        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<T> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
    }

    private Query.Filter prepareTimestampFilter(Timestamp from) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, from.getSeconds()));
        filters.add(new Query.FilterPredicate(TIMESTAMP_NANOS_KEY, GREATER_THAN_OR_EQUAL, from.getNanos()));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    private Query.Filter prepareAggregateRootIdAndTimestampFilter(Message aggregateRootId, Timestamp from) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, from.getSeconds()));
        filters.add(new Query.FilterPredicate(TIMESTAMP_NANOS_KEY, GREATER_THAN_OR_EQUAL, from.getNanos()));
        filters.add(new Query.FilterPredicate(AGGREGATE_ID_KEY, EQUAL, JsonFormat.printToString(aggregateRootId)));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    private Query.Filter prepareAggregateRootIdAndVersionFilter(Message aggregateRootId, int sinceVersion) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(AGGREGATE_ID_KEY, EQUAL, JsonFormat.printToString(aggregateRootId)));
        filters.add(new Query.FilterPredicate(VERSION_KEY, GREATER_THAN_OR_EQUAL, sinceVersion));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    private <T extends Message> T readMessageFromDataStore(String kind, String id) {
        final Key key = KeyFactory.createKey(kind, id);
        final Entity entity = readEntityFromDataStore(key);
        return readMessageFromEntity(entity);
    }

    private <T extends Message> List<T> readAllMessagesFromDataStoreByQuery(PreparedQuery query) {
        final List<T> messages = new ArrayList<>();

        for (Entity entity : query.asIterable()) {
            final T message = readMessageFromEntity(entity);
            messages.add(message);
        }

        return messages;
    }

    private Entity readEntityFromDataStore(Key key) {
        Entity entity;

        try {
            entity = dataStore.get(key);
        } catch (EntityNotFoundException e) {
            //todo:2015-07-15:mikhail.mikhaylov: Have a proper exception
            throw new RuntimeException("Could not find entity");
        }

        return entity;
    }

    private <T extends Message> T readMessageFromEntity(Entity entity) {
        final Blob messageBlob = (Blob) entity.getProperty(VALUE_KEY);
        final ByteString messageByteString = ByteString.copyFrom(messageBlob.getBytes());
        final String typeUrl = (String) entity.getProperty(TYPE_URL_KEY);

        final Any messageAny = Any.newBuilder().setValue(messageByteString).setTypeUrl(typeUrl).build();

        return Messages.fromAny(messageAny);
    }
}
