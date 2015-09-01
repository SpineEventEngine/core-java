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
import com.google.protobuf.TimestampOrBuilder;
import org.spine3.TypeName;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static org.spine3.protobuf.Messages.toJson;

/**
 * Provides the access to common part of working with DataStore.
 *
 * @author Mikhail Mikhaylov
 */
class DataStoreHelper {

    public static final String VALUE_KEY = "value";
    public static final String TYPE_KEY = "type";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String VERSION_KEY = "version";
    public static final String AGGREGATE_ID_KEY = "aggregate_id";
    public static final String PARENT_ID_KEY = "parent_id";

    private final DatastoreService dataStore;

    protected DataStoreHelper() {
        dataStore = DatastoreServiceFactory.getDatastoreService();
    }

    protected Key put(Entity entity) {
        return dataStore.put(entity);
    }

    protected <T extends Message> T read(TypeName kind, String id) {
        final Key key = KeyFactory.createKey(kind.toString(), id);
        final Entity entity = readEntity(key);
        return readMessageFromEntity(entity);
    }

    protected static Query.Filter prepareFilter(TimestampOrBuilder from) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, Timestamps.convertToDate(from)));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    protected <T extends Message> List<T> read(String kind) {
        return readByFilter(kind, null);
    }

    protected <T extends Message> List<T> readByFilter(String kind, @Nullable Query.Filter filter) {
        final Query query = new Query(kind);
        if (filter != null) {
            query.setFilter(filter);
        }

        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<T> messages = readAll(preparedQuery);

        return messages;
    }

    protected static Query.Filter prepareFilter(Message aggregateRootId, TimestampOrBuilder from) {

        final List<Query.Filter> filters = new ArrayList<>();
        final Date timestampDate = Timestamps.convertToDate(from);
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, timestampDate));
        filters.add(new Query.FilterPredicate(PARENT_ID_KEY, EQUAL, toJson(aggregateRootId)));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    protected static Query.Filter prepareFilter(Message aggregateRootId, int sinceVersion) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(PARENT_ID_KEY, EQUAL, toJson(aggregateRootId)));
        filters.add(new Query.FilterPredicate(VERSION_KEY, GREATER_THAN_OR_EQUAL, sinceVersion));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    private Entity readEntity(Key key) {
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
        final String typeUrl = (String) entity.getProperty(TYPE_KEY);

        final Any messageAny = Any.newBuilder().setValue(messageByteString).setTypeUrl(typeUrl).build();

        return Messages.fromAny(messageAny);
    }

    private static <T extends Message> List<T> readAll(PreparedQuery query) {
        final List<T> messages = new ArrayList<>();

        for (Entity entity : query.asIterable()) {
            final T message = readMessageFromEntity(entity);
            messages.add(message);
        }

        return messages;
    }
}
