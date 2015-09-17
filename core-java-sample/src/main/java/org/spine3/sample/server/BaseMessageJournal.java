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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.server.MessageJournal;
import org.spine3.util.Commands;
import org.spine3.util.Events;

import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Commands.*;
import static org.spine3.util.Events.*;
import static org.spine3.util.Lists.filter;

/**
 * Base implementation of the {@link com.google.protobuf.Message} repository.
 * Filters and sorts data sets.
 */
public abstract class BaseMessageJournal<I, M extends Message> implements MessageJournal<I, M> {

    private static final Map<Class<?>, FilteringHelper<?>> HELPERS = ImmutableMap.<Class<?>, FilteringHelper<?>>builder()
            .put(CommandRequest.class, new CommandFilteringHelper())
            .put(EventRecord.class, new EventFilteringHelper())
            .build();

    private final Class<M> messageClass;

    protected BaseMessageJournal(Class<M> messageClass) {
        this.messageClass = messageClass;
    }

    protected abstract void save(I entityId, M message);
    protected abstract List<M> getById(Class<M> messageClass, I parentId);
    protected abstract List<M> getAll(Class<M> messageClass);


    @Override
    public List<M> load(I entityId) {
        checkNotNull(entityId);

        final List<M> messages = getById(messageClass, entityId);

        return messages;
    }


    @Override
    public void store(I entityId, M message) {
        save(entityId, message);
    }

    @Override
    public List<M> loadSince(I entityId, Timestamp timestamp) {
        checkNotNull(timestamp);
        checkNotNull(entityId);

        final List<M> messages = getById(messageClass, entityId);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) HELPERS.get(messageClass);
        final Predicate<M> predicate = helper.getWereAfterPredicate(timestamp);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> loadAllSince(Timestamp timestamp) {
        checkNotNull(timestamp);

        final List<M> messages = getAll(messageClass);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) HELPERS.get(messageClass);
        final Predicate<M> predicate = helper.getWereAfterPredicate(timestamp);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }


    private static class CommandFilteringHelper implements FilteringHelper<CommandRequest> {

        private static final String COMMANDS_DO_NOT_SUPPORT_VERSIONS = "Commands don\'t support versions";

        @Override
        public Predicate<CommandRequest> getWereAfterPredicate(Timestamp from) {
            return wereAfter(from);
        }

        @Override
        public void sort(List<CommandRequest> messages) {
            Commands.sort(messages);
        }
    }

    private static class EventFilteringHelper implements FilteringHelper<EventRecord> {

        @Override
        public Predicate<EventRecord> getWereAfterPredicate(Timestamp from) {
            return getEventPredicate(from);
        }

        @Override
        public void sort(List<EventRecord> messages) {
            Events.sort(messages);
        }
    }

    private interface FilteringHelper<M extends Message> {

        Predicate<M> getWereAfterPredicate(Timestamp from);

        void sort(List<M> messages);
    }
}
