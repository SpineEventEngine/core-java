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

package org.spine3.sample.server;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.server.aggregate.AggregateCommand;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;
import static org.spine3.protobuf.Messages.fromAny;

/**
 * In-memory-based implementation of the {@link com.google.protobuf.Message} repository.
 */
public class InMemoryStorage<M extends Message> extends BaseStorage<M> {

    private final Map<Class, StorageHelper> storageHelpers = ImmutableMap.<Class, StorageHelper>builder()
            .put(CommandRequest.class, new CommandStorageHelper())
            .put(EventRecord.class, new EventStorageHelper())
            .build();

    private final Map<Message, List<CommandRequest>> commandRequestsMap = newHashMap();

    private final Map<Message, List<EventRecord>> eventRecordsMap = newHashMap();


    public InMemoryStorage(Class<M> messageClass) {
        super(messageClass);
    }

    @Override
    protected List<M> read(Class<M> messageClass, Message parentId) {

        final StorageHelper helper = storageHelpers.get(messageClass);
        //noinspection unchecked
        final List<M> result = helper.get(parentId);
        return result;
    }

    @Override
    protected List<M> readAll(Class<M> messageClass) {

        final StorageHelper helper = storageHelpers.get(messageClass);
        //noinspection unchecked
        final List<M> result = helper.getAll();
        return result;
    }

    @Override
    protected void save(M message) {

        final StorageHelper helper = storageHelpers.get(message.getClass());
        //noinspection unchecked
        helper.save(message);
    }


    private class CommandStorageHelper extends StorageHelper<CommandRequest> {

        @Override
        protected Message getAggregateId(CommandRequest message) {
            Message command = AggregateCommand.getCommandValue(message);
            final Message aggregateId = AggregateCommand.getAggregateId(command).value();
            return aggregateId;
        }

        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        protected Map<Message, List<CommandRequest>> getStorage() {
            return commandRequestsMap;
        }
    }

    private class EventStorageHelper extends StorageHelper<EventRecord> {

        @Override
        protected Message getAggregateId(EventRecord record) {
            final Message aggregateId = fromAny(record.getContext().getAggregateId());
            return aggregateId;
        }

        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        protected Map<Message, List<EventRecord>> getStorage() {
            return eventRecordsMap;
        }
    }

    private abstract static class StorageHelper<M extends Message> {

        protected abstract Message getAggregateId(M message);
        protected abstract Map<Message, List<M>> getStorage();

        private void save(M message) {

            Message aggregateId = getAggregateId(message);
            final Map<Message, List<M>> storage = getStorage();

            List<M> messagesById = newArrayList();

            if (storage.containsKey(aggregateId)) {
                messagesById = storage.get(aggregateId);
            }

            messagesById.add(message);
            storage.put(aggregateId, messagesById);
        }

        private List<M> get(Message aggregateId) {

            List<M> result = newArrayList();
            final Map<Message, List<M>> storage = getStorage();

            if (storage.containsKey(aggregateId)) {
                result = storage.get(aggregateId);
            }

            return result;
        }

        private List<M> getAll() {

            final List<M> result = newLinkedList();
            final Map<Message, List<M>> storage = getStorage();

            for (Message key : storage.keySet()){
                final List<M> messages = storage.get(key);
                result.addAll(messages);
            }

            return result;
        }
    }
}
