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
import com.google.protobuf.*;
import org.spine3.gae.lang.MissingEntityException;
import org.spine3.util.JsonFormat;
import org.spine3.util.Messages;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;

/**
 * Provides the access to common part of working with DataStore.
 *
 * @author Mikhail Mikhaylov
 */
class DataStoreHelper {

    public static final String VALUE_KEY = "value";
    public static final String TYPE_URL_KEY = "type_url";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String TIMESTAMP_NANOS_KEY = "timestamp_nanos";
    public static final String VERSION_KEY = "version";
    public static final String PARENT_ID_KEY = "parent_id";
    public static final String SINGLETON_ID_KEY = "singleton_id";

    public static final String SINGLETON_KIND = "singleton";

    private final DatastoreService dataStore;

    protected DataStoreHelper() {
        dataStore = DatastoreServiceFactory.getDatastoreService();
    }

    protected Key put(Entity entity) {
        return dataStore.put(entity);
    }

    protected <T extends Message> T readMessageFromDataStore(String kind, String id) {
        final Key key = KeyFactory.createKey(kind, id);
        final Entity entity = readEntityFromDataStore(key);
        return readMessageFromEntity(entity);
    }

    protected static Query.Filter prepareTimestampFilter(Timestamp from) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, from.getSeconds()));
        filters.add(new Query.FilterPredicate(TIMESTAMP_NANOS_KEY, GREATER_THAN_OR_EQUAL, from.getNanos()));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    protected <T extends Message> List<T> readMessagesFromDataStore(String kind) {
        return readMessagesFromDataStore(kind, null);
    }

    protected <T extends Message> List<T> readMessagesFromDataStore(String kind, @Nullable Query.Filter filter) {
        final Query query = new Query(kind);
        if (filter != null) {
            query.setFilter(filter);
        }

        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<T> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
    }

    protected static Query.Filter prepareAggregateRootIdAndTimestampFilter(Message aggregateRootId, Timestamp from) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, from.getSeconds()));
        filters.add(new Query.FilterPredicate(TIMESTAMP_NANOS_KEY, GREATER_THAN_OR_EQUAL, from.getNanos()));
        filters.add(new Query.FilterPredicate(PARENT_ID_KEY, EQUAL, JsonFormat.printToString(aggregateRootId)));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    protected static Query.Filter prepareAggregateRootIdAndVersionFilter(Message aggregateRootId, int sinceVersion) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(PARENT_ID_KEY, EQUAL, JsonFormat.printToString(aggregateRootId)));
        filters.add(new Query.FilterPredicate(VERSION_KEY, GREATER_THAN_OR_EQUAL, sinceVersion));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    private Entity readEntityFromDataStore(Key key) {
        Entity entity;

        try {
            entity = dataStore.get(key);
        } catch (EntityNotFoundException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw new MissingEntityException(key, e.getCause());
        }

        return entity;
    }

    private static <T extends Message> T readMessageFromEntity(Entity entity) {
        final Blob messageBlob = (Blob) entity.getProperty(VALUE_KEY);
        final ByteString messageByteString = ByteString.copyFrom(messageBlob.getBytes());

        Any messageAny = null;
        try {
            messageAny = Any.parseFrom(messageByteString);
        } catch (InvalidProtocolBufferException e) {
            //NOP
        }

        //noinspection ConstantConditions
        return Messages.fromAny(messageAny);
    }

    private static <T extends Message> List<T> readAllMessagesFromDataStoreByQuery(PreparedQuery query) {
        final List<T> messages = new ArrayList<>();

        for (Entity entity : query.asIterable()) {
            final T message = readMessageFromEntity(entity);
            messages.add(message);
        }

        return messages;
    }
}
