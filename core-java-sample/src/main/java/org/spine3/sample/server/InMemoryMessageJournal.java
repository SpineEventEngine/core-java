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

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * In-memory-based implementation of the {@link com.google.protobuf.Message} repository.
 */
public class InMemoryMessageJournal<I, M extends Message> extends BaseMessageJournal<I, M> {

    private final Map<Class, StorageHelper> storageHelpers = ImmutableMap.<Class, StorageHelper>builder()
            .put(CommandRequest.class, new CommandStorageHelper())
            .put(EventRecord.class, new EventStorageHelper())
            .build();

    private final Map<I, List<CommandRequest>> commandRequestsMap = newHashMap();

    private final Map<I, List<EventRecord>> eventRecordsMap = newHashMap();


    public InMemoryMessageJournal(Class<M> messageClass) {
        super(messageClass);
    }

    @Override
    protected List<M> getById(Class<M> messageClass, I parentId) {

        final StorageHelper helper = storageHelpers.get(messageClass);
        //noinspection unchecked
        final List<M> result = helper.getById(parentId);
        return result;
    }

    @Override
    protected List<M> getAll(Class<M> messageClass) {

        final StorageHelper helper = storageHelpers.get(messageClass);
        //noinspection unchecked
        final List<M> result = helper.getAll();
        return result;
    }

    @Override
    protected void save(I entityId, M message) {

        final StorageHelper helper = storageHelpers.get(message.getClass());
        //noinspection unchecked
        helper.save(entityId, message);
    }


    private class CommandStorageHelper extends StorageHelper<I, CommandRequest> {

        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        protected Map<I, List<CommandRequest>> getStorage() {
            return commandRequestsMap;
        }
    }

    private class EventStorageHelper extends StorageHelper<I, EventRecord> {

        @Override
        @SuppressWarnings("ReturnOfCollectionOrArrayField")
        protected Map<I, List<EventRecord>> getStorage() {
            return eventRecordsMap;
        }
    }

    private abstract static class StorageHelper<I, M extends Message> {

        protected abstract Map<I, List<M>> getStorage();

        private void save(I entityId, M message) {

            final Map<I, List<M>> storage = getStorage();

            List<M> messagesById = newArrayList();

            if (storage.containsKey(entityId)) {
                messagesById = storage.get(entityId);
            }

            messagesById.add(message);
            storage.put(entityId, messagesById);
        }

        private List<M> getById(I id) {

            List<M> result = newArrayList();
            final Map<I, List<M>> storage = getStorage();

            if (storage.containsKey(id)) {
                result = storage.get(id);
            }

            return result;
        }

        private List<M> getAll() {

            final List<M> result = newLinkedList();
            final Map<I, List<M>> storage = getStorage();

            for (I key : storage.keySet()){
                final List<M> messages = storage.get(key);
                result.addAll(messages);
            }

            return result;
        }
    }
}
