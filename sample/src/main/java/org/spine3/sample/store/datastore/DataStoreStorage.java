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

package org.spine3.sample.store.datastore;

import com.google.appengine.api.datastore.*;
import com.google.protobuf.*;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.base.Snapshot;
import org.spine3.engine.AbstractStorage;

import org.spine3.util.Commands;
import org.spine3.util.JsonFormat;
import org.spine3.util.Messages;

import java.util.ArrayList;
import java.util.List;

import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.GREATER_THAN_OR_EQUAL;
import static com.google.appengine.api.datastore.Query.FilterOperator.LESS_THAN_OR_EQUAL;

/**
 * @author Mikhail Mikhaylov.
 */
public class DataStoreStorage extends AbstractStorage {

    private static final String VALUE_KEY = "value";
    private static final String TYPE_URL_KEY = "type_url";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String VERSION_KEY = "version";
    private static final String AGGREGATE_ID_KEY = "aggregate_id";

    private static final String COMMAND_KIND = "command";
    private static final String EVENT_KIND = "event";
    private static final String SNAPSHOT_KIND = "snapshot";

    private final DatastoreService dataStore;

    public DataStoreStorage() {
        dataStore = DatastoreServiceFactory.getDatastoreService();
    }

    @Override
    public List<EventRecord> readEvents(Message aggregateId) {
        //todo:2015-07-16:mikhail.mikhaylov: encapsulate query
        final Query query = new Query(EVENT_KIND).setFilter(
                new Query.FilterPredicate(AGGREGATE_ID_KEY, EQUAL, JsonFormat.printToString(aggregateId)));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<EventRecord> eventRecords = readAllMessagesFromDataStoreByQuery(preparedQuery);
        return eventRecords;
    }

    @Override
    public List<CommandRequest> readCommands(Message aggregateId) {
        final Query query = new Query(COMMAND_KIND).setFilter(
                new Query.FilterPredicate(AGGREGATE_ID_KEY, EQUAL, JsonFormat.printToString(aggregateId)));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<CommandRequest> commandRequests = readAllMessagesFromDataStoreByQuery(preparedQuery);
        return commandRequests;
    }

    @Override
    public List<EventRecord> readEvents(int sinceVersion) {
        final Query query = new Query(EVENT_KIND).setFilter(
                new Query.FilterPredicate(VERSION_KEY, GREATER_THAN_OR_EQUAL, sinceVersion));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<EventRecord> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);
        return messages;
    }

    @Override
    public List<EventRecord> readEvents(Timestamp from) {
        final Query query = new Query(EVENT_KIND).setFilter(prepareTimestampFilter(from));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<EventRecord> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
    }

    @Override
    public List<CommandRequest> readCommands(Timestamp from) {
        final Query query = new Query(COMMAND_KIND).setFilter(prepareTimestampFilter(from));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<CommandRequest> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
    }

    @Override
    public List<EventRecord> readEvents(Timestamp from, Timestamp to) {
        final Query query = new Query(EVENT_KIND).setFilter(prepareTimestampFilter(from, to));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<EventRecord> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
    }

    @Override
    public List<CommandRequest> readCommands(Timestamp from, Timestamp to) {
        final Query query = new Query(COMMAND_KIND).setFilter(prepareTimestampFilter(from, to));
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<CommandRequest> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
    }

    @Override
    public List<EventRecord> readAllEvents() {
        final List<EventRecord> events = readAllMessagesFromDataStoreByKind(EVENT_KIND);
        return events;
    }

    @Override
    public List<CommandRequest> readAllCommands() {
        final List<CommandRequest> commands = readAllMessagesFromDataStoreByKind(COMMAND_KIND);
        return commands;
    }

    @Override
    public Snapshot readLastSnapshot(Message aggregateId) {
        final String kind = SNAPSHOT_KIND;
        final String id = JsonFormat.printToString(aggregateId);

        final Snapshot snapshot = readMessageFromDataStore(kind, id);

        return snapshot;
    }

    @Override
    public void writeEvent(EventRecord eventRecord) {
        final Message aggregateId = Messages.fromAny(eventRecord.getContext().getAggregateId());
        final String kind = EVENT_KIND;
        final String id = JsonFormat.printToString(eventRecord.getContext().getEventId());

        final Entity dataStoreEntity = new Entity(kind, id);

        final Any any = Messages.toAny(eventRecord);

        dataStoreEntity.setProperty(VALUE_KEY, new Blob(any.getValue().toByteArray()));
        dataStoreEntity.setProperty(TYPE_URL_KEY, any.getTypeUrl());
        dataStoreEntity.setProperty(AGGREGATE_ID_KEY, JsonFormat.printToString(aggregateId));
        dataStoreEntity.setProperty(TIMESTAMP_KEY, eventRecord.getContext().getEventId().getTimestamp().getSeconds());
        dataStoreEntity.setProperty(VERSION_KEY, eventRecord.getContext().getVersion());

        dataStore.put(dataStoreEntity);
    }

    @Override
    public void writeCommand(CommandRequest commandRequest) {
        final Message command = Messages.fromAny(commandRequest.getCommand());
        final Message aggregateId = Commands.getAggregateId(command);

        final String kind = COMMAND_KIND;
        final String id = JsonFormat.printToString(commandRequest.getContext().getCommandId());

        final Entity dataStoreEntity = new Entity(kind, id);


        final Any any = Messages.toAny(commandRequest);

        dataStoreEntity.setProperty(VALUE_KEY, new Blob(any.getValue().toByteArray()));
        dataStoreEntity.setProperty(TYPE_URL_KEY, any.getTypeUrl());
        dataStoreEntity.setProperty(AGGREGATE_ID_KEY, JsonFormat.printToString(aggregateId));
        dataStoreEntity.setProperty(TIMESTAMP_KEY, commandRequest.getContext().getCommandId().getTimestamp().getSeconds());

        dataStore.put(dataStoreEntity);
    }

    @Override
    public void writeSnapshot(Message aggregateId, Snapshot snapshot) {
        final String kind = SNAPSHOT_KIND;
        final String id = JsonFormat.printToString(aggregateId);

        final Entity dataStoreEntity = new Entity(kind, id);

        final Any any = Messages.toAny(snapshot);

        dataStoreEntity.setProperty(VALUE_KEY, new Blob(any.getValue().toByteArray()));
        dataStoreEntity.setProperty(TYPE_URL_KEY, any.getTypeUrl());

        dataStore.put(dataStoreEntity);
    }

    private Query.Filter prepareTimestampFilter(Timestamp from) {
        return new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, from.getSeconds());
    }

    private Query.Filter prepareTimestampFilter(Timestamp from, Timestamp to) {

        final List<Query.Filter> filters = new ArrayList<>();
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, GREATER_THAN_OR_EQUAL, from.getSeconds()));
        filters.add(new Query.FilterPredicate(TIMESTAMP_KEY, LESS_THAN_OR_EQUAL, to.getSeconds()));

        return new Query.CompositeFilter(Query.CompositeFilterOperator.AND, filters);
    }

    private <T extends Message> T readMessageFromDataStore(String kind, String id) {
        final Key key = KeyFactory.createKey(kind, id);
        final Entity entity = readEntityFromDataStore(key);
        return readMessageFromEntity(entity);
    }

    private <T extends Message> List<T> readAllMessagesFromDataStoreByKind(String kind) {
        final Query query = new Query(kind);
        final PreparedQuery preparedQuery = dataStore.prepare(query);

        final List<T> messages = readAllMessagesFromDataStoreByQuery(preparedQuery);

        return messages;
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
