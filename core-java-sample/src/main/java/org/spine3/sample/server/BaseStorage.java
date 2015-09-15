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
import org.spine3.server.StorageWithTimelineAndVersion;
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
public abstract class BaseStorage<M extends Message> implements StorageWithTimelineAndVersion<M> {

    private static final Map<Class<?>, FilteringHelper<?>> HELPERS = ImmutableMap.<Class<?>, FilteringHelper<?>>builder()
            .put(CommandRequest.class, new CommandFilteringHelper())
            .put(EventRecord.class, new EventFilteringHelper())
            .build();

    private final Class<M> messageClass;

    protected BaseStorage(Class<M> messageClass) {
        this.messageClass = messageClass;
    }

    protected abstract List<M> read(Class<M> messageClass, Message parentId);
    protected abstract List<M> readAll(Class<M> messageClass);
    protected abstract void save(M message);


    @Override
    public List<M> read(Message parentId, int sinceVersion) {

        checkNotNull(parentId);

        final List<M> messages = read(messageClass, parentId);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) HELPERS.get(messageClass);
        final Predicate<M> predicate = helper.getSinceVersionPredicate(sinceVersion);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> read(Timestamp from) {

        checkNotNull(from);

        final List<M> messages = readAll(messageClass);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) HELPERS.get(messageClass);
        final Predicate<M> predicate = helper.getWereAfterPredicate(from);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> read(Message parentId, Timestamp from) {

        checkNotNull(from);
        checkNotNull(parentId);

        final List<M> messages = read(messageClass, parentId);

        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) HELPERS.get(messageClass);
        final Predicate<M> predicate = helper.getWereAfterPredicate(from);
        final ImmutableList<M> result = filter(messages, predicate);
        return result;
    }

    @Override
    public List<M> read(Message parentId) {

        checkNotNull(parentId);
        final List<M> messages = read(messageClass, parentId);
        return messages;
    }

    @Override
    public List<M> readAll() {

        final List<M> messages = readAll(messageClass);
        //noinspection unchecked
        final FilteringHelper<M> helper = (FilteringHelper<M>) HELPERS.get(messageClass);
        helper.sort(messages);
        return messages;
    }

    @Override
    public void store(M message) {
        save(message);
    }


    private static class CommandFilteringHelper implements FilteringHelper<CommandRequest> {

        private static final String COMMANDS_DO_NOT_SUPPORT_VERSIONS = "Commands don\'t support versions";

        @Override
        public Predicate<CommandRequest> getWereAfterPredicate(Timestamp from) {
            return wereAfter(from);
        }

        @Override
        public Predicate<CommandRequest> getSinceVersionPredicate(int sinceVersion) {
            throw new IllegalStateException(COMMANDS_DO_NOT_SUPPORT_VERSIONS);
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
        public Predicate<EventRecord> getSinceVersionPredicate(int sinceVersion) {
            return getEventPredicate(sinceVersion);
        }

        @Override
        public void sort(List<EventRecord> messages) {
            Events.sort(messages);
        }
    }

    private interface FilteringHelper<M extends Message> {

        Predicate<M> getWereAfterPredicate(Timestamp from);

        Predicate<M> getSinceVersionPredicate(int sinceVersion);

        void sort(List<M> messages);
    }
}
